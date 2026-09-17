import test from "node:test";
import assert from "node:assert/strict";
import bcrypt from "bcrypt";
import User from "../models/user.model.js";
import OTP from "../models/otp.model.js";
import AuthGrant from "../models/authGrant.model.js";
import { transporter } from "../config/email.js";
import { sendOtp, verifyOtp, createAccount, login, logout, resetPassword, sendPasswordResetOtp, verifyPasswordResetOtp } from "../controllers/user.controller.js";
import { protectRoute } from "../middleware/auth.middleware.js";
import { digest, verifySession } from "../services/authSecurity.js";

process.env.JWT_SECRET_KEY = "offline-fixture-signing-key-49708d83b9334658b469";
const email = "fixture@example.invalid";
const proof = "a".repeat(64);
const response = () => ({ statusCode: 200, cookies: [], status(code) { this.statusCode = code; return this; }, cookie(...args) { this.cookies.push(args); return this; }, clearCookie(...args) { this.cleared = args; }, json(body) { this.body = JSON.parse(JSON.stringify(body)); return this; } });

test("OTP verification gives a hashed one-use signup proof; signup/login/protected HTTP work offline", async (t) => {
  let record = { _id: "otp-id", otpHash: digest("123456"), attempts: 0 };
  let grant;
  let user;
  t.mock.method(OTP, "findOneAndUpdate", (filter) => {
    assert.equal(filter.isForgotPassword, false);
    assert.equal(filter.attempts.$lt, 5);
    return { select: async () => record };
  });
  t.mock.method(OTP, "findOneAndDelete", async (filter) => {
    assert.equal(filter.otpHash, digest("123456"));
    const result = record; record = null; return result;
  });
  t.mock.method(AuthGrant, "findOneAndUpdate", async (filter, update) => { grant = { ...filter, ...update.$set }; });
  const verified = response();
  await verifyOtp({ body: { email, otp: "123456" } }, verified);
  assert.equal(verified.body.success, true);
  const signupToken = verified.body.signupToken;
  assert.match(signupToken, /^[a-f0-9]{64}$/);
  assert.equal(grant.tokenHash, digest(signupToken));
  assert.notEqual(grant.tokenHash, signupToken);
  t.mock.method(User, "findOne", () => ({ then(resolve) { return Promise.resolve(user).then(resolve); }, select: async () => user }));
  t.mock.method(AuthGrant, "findOneAndDelete", async (filter) => {
    if (!grant || filter.email !== grant.email || filter.tokenHash !== grant.tokenHash) return null;
    assert.equal(filter.purpose, "signup");
    assert.ok(filter.expiresAt.$gt instanceof Date);
    const result = grant; grant = null; return result;
  });
  t.mock.method(User, "create", async (data) => { user = new User(data); return user; });
  const signup = response();
  await createAccount({ body: { email, fullName: "Fixture", password: "correct-password", signupToken } }, signup);
  assert.equal(signup.body.success, true);
  assert.equal(signup.body.user.isVerified, true);
  assert.equal(signup.body.user.password, undefined);
  assert.equal(await bcrypt.compare("correct-password", user.password), true);
  assert.equal(verifySession(signup.body.token).sessionVersion, 0);
  assert.equal(signup.cookies[0][2].httpOnly, true);
  assert.equal(grant, null);
  const replay = response();
  await verifyOtp({ body: { email, otp: "123456" } }, replay);
  assert.equal(replay.statusCode, 400);
  const loggedIn = response();
  await login({ body: { email, password: "correct-password" } }, loggedIn);
  assert.equal(loggedIn.statusCode, 201);
  t.mock.method(User, "findById", () => ({ select: async () => user }));
  const req = { cookies: { token: loggedIn.body.token } };
  let nextCalled = false;
  await protectRoute(req, response(), () => { nextCalled = true; });
  assert.equal(nextCalled, true);
  assert.equal(req.user.id, user.id);
});

for (const [name, handler, body] of [
  ["signup without proof", createAccount, { email, fullName: "Fixture", password: "password123" }],
  ["signup email operator", createAccount, { email: { $ne: null }, signupToken: proof, password: "password123" }],
  ["login email operator", login, { email: { $ne: null }, password: "password123" }],
  ["login password operator", login, { email, password: { $gt: "" } }],
  ["OTP operator", verifyOtp, { email, otp: { $ne: null } }],
  ["OTP send array", sendOtp, { email: [email] }],
  ["reset email operator", sendPasswordResetOtp, { email: { $ne: null } }],
  ["reset OTP array", verifyPasswordResetOtp, { email, otp: ["123456"] }],
  ["reset token operator", resetPassword, { token: { $ne: null }, password: "password123" }],
  ["short new password", createAccount, { email, signupToken: proof, fullName: "Fixture", password: "1234567" }],
  ["bcrypt truncation", resetPassword, { token: proof, password: "界".repeat(25) }],
]) {
  test(`${name} is rejected before any database operation`, async (t) => {
    for (const model of [User, OTP, AuthGrant]) for (const method of ["findOne", "findOneAndUpdate", "findOneAndDelete"]) t.mock.method(model, method, () => assert.fail("Unexpected database access"));
    const res = response(); await handler({ body }, res); assert.equal(res.statusCode, 400);
  });
}


test("signup rejects guessed, mismatched, expired and consumed proofs without creating users", async (t) => {
  t.mock.method(User, "findOne", async () => null);
  t.mock.method(User, "create", () => assert.fail("No account may be created"));
  t.mock.method(AuthGrant, "findOneAndDelete", async (filter) => {
    assert.equal(filter.email, email);
    assert.equal(filter.tokenHash, digest(proof));
    assert.equal(filter.purpose, "signup");
    assert.ok(filter.expiresAt.$gt instanceof Date);
    return null;
  });
  const res = response();
  await createAccount({ body: { email, signupToken: proof, fullName: "Fixture", password: "password123" } }, res);
  assert.equal(res.statusCode, 400);
  assert.equal(res.cookies.length, 0);
});

test("OTP send uses hashed six digit code and fails visibly on provider error", async (t) => {
  t.mock.method(User, "findOne", async () => null);
  let storedHash;
  t.mock.method(OTP, "findOneAndUpdate", async (filter, update, options) => {
    assert.equal(filter.email, email);
    assert.equal(filter.isForgotPassword, false);
    assert.ok(filter.$or[0].issuedAt.$lte instanceof Date);
    assert.equal(options.upsert, true);
    assert.equal(update.$set.attempts, 0);
    assert.equal(update.$set.otp, undefined);
    storedHash = update.$set.otpHash;
    return { _id: "otp-id" };
  });
  t.mock.method(AuthGrant, "deleteMany", async () => ({}));
  t.mock.method(transporter, "sendMail", async ({ html }) => {
    const code = html.match(/<strong>(\d{6})<\/strong>/)[1];
    assert.equal(storedHash, digest(code));
    throw new Error("provider-private-details");
  });
  const invalidate = t.mock.method(OTP, "updateOne", async (filter, update) => {
    assert.equal(filter.otpHash, storedHash);
    assert.equal(update.$set.attempts, 5);
  });
  const res = response(); await sendOtp({ body: { email } }, res);
  assert.equal(res.statusCode, 503);
  assert.equal(res.body.success, false);
  assert.equal(invalidate.mock.callCount(), 1);
  assert.ok(!JSON.stringify(res.body).includes("provider-private-details"));
});

test("OTP cooldown duplicate race returns 429 instead of sending another message", async (t) => {
  t.mock.method(User, "findOne", async () => null);
  t.mock.method(OTP, "findOneAndUpdate", async () => { throw Object.assign(new Error(), { code: 11000 }); });
  const mail = t.mock.method(transporter, "sendMail", () => assert.fail("No delivery"));
  const res = response(); await sendOtp({ body: { email } }, res);
  assert.equal(res.statusCode, 429);
  assert.equal(mail.mock.callCount(), 0);
});

test("five wrong OTP attempts exhaust the code; purpose and attempt guards remain in each query", async (t) => {
  let attempts = 0;
  t.mock.method(OTP, "findOneAndUpdate", (filter, update) => ({ select: async () => {
    assert.equal(filter.isForgotPassword, true);
    assert.equal(filter.attempts.$lt, 5);
    assert.equal(update.$inc.attempts, 1);
    assert.ok(filter.expiresAt.$gt instanceof Date);
    return attempts++ < 5 ? { otpHash: digest("123456") } : null;
  } }));
  t.mock.method(AuthGrant, "findOneAndUpdate", () => assert.fail("No grant"));
  for (let i = 0; i < 6; i++) {
    const res = response();
    await verifyPasswordResetOtp({ body: { email, otp: i === 5 ? "123456" : "654321" } }, res);
    assert.equal(res.statusCode, 400);
  }
});

test("reset atomically consumes a hashed grant, changes password and revokes all sessions", async (t) => {
  let consumed = false;
  const user = new User({ email, fullName: "Fixture", sessionVersion: 2 });
  t.mock.method(AuthGrant, "findOneAndDelete", async (filter) => {
    assert.equal(filter.tokenHash, digest(proof)); assert.equal(filter.purpose, "reset");
    if (consumed) return null; consumed = true; return { email };
  });
  t.mock.method(User, "findOneAndUpdate", async (filter, update) => {
    assert.deepEqual(filter, { email });
    assert.equal(await bcrypt.compare("new-password", update.$set.password), true);
    assert.equal(update.$inc.sessionVersion, 1);
    return user;
  });
  let disconnected = false;
  const app = { get: () => ({ in: (room) => { assert.equal(room, `auth-session:${user.id}`); return { disconnectSockets: () => { disconnected = true; } }; } }) };
  const res = response(); await resetPassword({ app, body: { token: proof, password: "new-password" } }, res);
  assert.equal(res.body.success, true);
  assert.equal(disconnected, true);
  assert.equal(res.cleared[1].httpOnly, true);
  const replay = response(); await resetPassword({ app, body: { token: proof, password: "new-password" } }, replay);
  assert.equal(replay.statusCode, 400);
});

test("logout increments the persisted version, disconnects sessions and clears secure cookie", async (t) => {
  const user = new User({ email, fullName: "Fixture" });
  t.mock.method(User, "updateOne", async (filter, update) => {
    assert.equal(filter._id, user._id); assert.deepEqual(update, { $inc: { sessionVersion: 1 } });
  });
  let disconnected = false;
  const app = { get: () => ({ in: () => ({ disconnectSockets: () => { disconnected = true; } }) }) };
  const res = response(); await logout({ user, app }, res);
  assert.equal(res.body.success, true);
  assert.equal(disconnected, true);
  assert.equal(res.cleared[0], "token");
  assert.equal(res.cleared[1].sameSite, "lax");
});
