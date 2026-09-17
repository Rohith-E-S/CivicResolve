import test from "node:test";
import assert from "node:assert/strict";
import jwt from "jsonwebtoken";
import User from "../models/user.model.js";
import AuthRateLimit from "../models/authRateLimit.model.js";
import { signSession, verifySession, validPassword, cookieOptions } from "../services/authSecurity.js";
import { protectRoute } from "../middleware/auth.middleware.js";
import { socketAuth } from "../middleware/socket.auth.js";
import { authRateLimit } from "../middleware/authRateLimit.js";

const key = "offline-fixture-signing-key-49708d83b9334658b469";
process.env.JWT_SECRET_KEY = key;
const user = () => new User({ email: "fixture@example.invalid", fullName: "Fixture", sessionVersion: 2 });
const response = () => ({ statusCode: 200, status(code) { this.statusCode = code; return this; }, json(body) { this.body = body; return this; }, set(key, value) { this[key] = value; } });

test("weak/missing secrets fail closed at signing and verifying; no algorithm downgrade", () => {
  const fixture = user(); const token = signSession(fixture);
  try {
    for (const weak of [undefined, "short", "x".repeat(64), "please-change-this-secret-in-production"]) {
      if (weak === undefined) delete process.env.JWT_SECRET_KEY; else process.env.JWT_SECRET_KEY = weak;
      assert.throws(() => signSession(fixture)); assert.throws(() => verifySession(token));
    }
  } finally { process.env.JWT_SECRET_KEY = key; }
  assert.throws(() => verifySession(jwt.sign({ _id: fixture.id, sessionVersion: 2 }, key, { algorithm: "HS384", expiresIn: "1d" })));
  assert.throws(() => verifySession(jwt.sign({ _id: fixture.id }, key, { expiresIn: "1d" })));
});

test("plaintext password policy counts characters and UTF-8 bytes before bcrypt", () => {
  assert.equal(validPassword("12345678"), true);
  assert.equal(validPassword("1234567"), false);
  assert.equal(validPassword("界".repeat(24)), true);
  assert.equal(validPassword("界".repeat(25)), false);
  assert.equal(validPassword("a".repeat(73)), false);
  assert.equal(validPassword({ $ne: null }), false);
});

test("production and development cookie options are secure and matching", () => {
  const old = process.env.NODE_ENV;
  try {
    process.env.NODE_ENV = "production";
    assert.deepEqual(cookieOptions(), { httpOnly: true, sameSite: "lax", secure: true, path: "/" });
    process.env.NODE_ENV = "development"; assert.equal(cookieOptions().secure, false);
  } finally { if (old === undefined) delete process.env.NODE_ENV; else process.env.NODE_ENV = old; }
});

test("HTTP rejects revoked, deleted, expired, and legacy sessions; bearer Android session succeeds", async (t) => {
  const fixture = user(); let current = fixture;
  t.mock.method(User, "findById", () => ({ select: async () => current }));
  const token = fixture.getJWT();
  const req = { headers: { authorization: `Bearer ${token}` } };
  let allowed = false; await protectRoute(req, response(), () => { allowed = true; });
  assert.equal(allowed, true);
  fixture.sessionVersion++;
  for (const bad of [token, jwt.sign({ _id: fixture.id }, key, { expiresIn: "1d" }), jwt.sign({ _id: fixture.id, sessionVersion: 3 }, key, { expiresIn: -1 })]) {
    const res = response(); await protectRoute({ cookies: { token: bad } }, res, () => assert.fail("Must reject"));
    assert.equal(res.statusCode, 401);
  }
  current = null;
  const res = response(); await protectRoute(req, res, () => assert.fail("Deleted account"));
  assert.equal(res.statusCode, 401);
});

function fakeSocket(handshake) {
  return { handshake, disconnected: false, join(room) { this.room = room; }, use(fn) { this.packet = fn; }, once(_event, fn) { this.cleanup = fn; }, disconnect() { this.disconnected = true; this.cleanup?.(); } };
}

for (const transport of ["cookie", "android"]) test(`socket ${transport} handshake validates and every packet rechecks version`, async (t) => {
  const fixture = user(); const token = fixture.getJWT();
  t.mock.method(User, "findById", () => ({ select: async () => fixture }));
  const socket = fakeSocket(transport === "cookie" ? { headers: { cookie: `other=value; token=${token}` } } : { auth: { token } });
  await socketAuth(socket, (error) => assert.equal(error, undefined));
  t.after(() => socket.disconnect());
  assert.equal(socket.room, `auth-session:${fixture.id}`);
  let packets = 0;
  await socket.packet(["sendMessage", {}], () => { packets++; }); assert.equal(packets, 1);
  fixture.sessionVersion++;
  await socket.packet(["sendMessage", {}], () => { packets++; });
  assert.equal(packets, 1); assert.equal(socket.disconnected, true);
});

test("long-lived socket packets recheck expiry, not just database version", async (t) => {
  const fixture = user();
  t.mock.method(User, "findById", () => ({ select: async () => fixture }));
  const socket = fakeSocket({ auth: { token: fixture.getJWT() } });
  await socketAuth(socket, (error) => assert.equal(error, undefined));
  t.after(() => socket.disconnect());
  const future = Date.now() + 2 * 86400000;
  t.mock.method(Date, "now", () => future);
  await socket.packet(["join_room"], () => assert.fail("Expired packet"));
  assert.equal(socket.disconnected, true);
});


test("rate limits persist hashed IP/account counters and fail closed on datastore outage", async (t) => {
  const counts = new Map();
  t.mock.method(AuthRateLimit, "findOneAndUpdate", async ({ _id }, update) => {
    assert.match(_id, /^[a-f0-9]{64}$/);
    assert.equal(update.$inc.count, 1);
    const count = (counts.get(_id) || 0) + 1; counts.set(_id, count); return { count };
  });
  const limiter = authRateLimit("fixture", { accountLimit: 2, ipLimit: 3 });
  const run = async (ip, email) => { const res = response(); await limiter({ ip, body: { email } }, res, () => {}); return res; };
  assert.equal((await run("1", "fixture@example.invalid")).statusCode, 200);
  assert.equal((await run("2", "fixture@example.invalid")).statusCode, 200);
  const blocked = await run("3", "fixture@example.invalid");
  assert.equal(blocked.statusCode, 429); assert.ok(Number(blocked["Retry-After"]) > 0);
  assert.equal((await run("1", "other@example.invalid")).statusCode, 200);
  assert.equal((await run("1", "another@example.invalid")).statusCode, 200);
  assert.equal((await run("1", "yet-another@example.invalid")).statusCode, 429);
  t.mock.method(AuthRateLimit, "findOneAndUpdate", async () => { throw new Error("offline"); });
  assert.equal((await run("4", "other@example.invalid")).statusCode, 503);
});
