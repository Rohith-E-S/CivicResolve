// Regression test: authentication secrets must never appear in serialized
// user responses, even for newly created documents or documents fetched with
// an explicit `+password` projection (the login code path).
// Runs fully offline: constructs documents in memory, no database connection.
import test from "node:test";
import assert from "node:assert/strict";
import bcrypt from "bcrypt";

process.env.JWT_SECRET_KEY = "offline-fixture-signing-key-49708d83b9334658b469";

const { default: User } = await import("../models/user.model.js");
const jwt = (await import("jsonwebtoken")).default;

const SECRET_FIELDS = [
  "password",
  "otp",
  "resetPasswordToken",
  "resetPasswordExpires",
  "fcmToken",
  "sessionVersion",
];

const buildUserWithPassword = async (password) => {
  const user = new User({
    email: "audit-case@example.com",
    fullName: "Serialization Regression Fixture",
    address: "fixture-address",
    isAdmin: false,
    isVerified: true,
    rank: "citizen",
    civicPoints: 10,
    otp: 123456,
    resetPasswordToken: "fixture-reset-proof",
    resetPasswordExpires: new Date(Date.now() + 60000),
    fcmToken: "fixture-device-token",
    sessionVersion: 4,
  });
  // Same shape login() produces after findOne({ email }).select("+password")
  user.set("password", await bcrypt.hash(password, 10));
  return user;
};

test("newly created user documents serialize without auth secrets (signup path)", async () => {
  const json = (await buildUserWithPassword("signup-password")).toJSON();
  for (const field of SECRET_FIELDS) {
    assert.ok(!(field in json), `${field} leaked via toJSON on signup response`);
  }
  assert.equal(json.email, "audit-case@example.com");
  assert.equal(json.fullName, "Serialization Regression Fixture");
});

test("explicitly selected documents still strip secrets (login path toObject/toJSON)", async () => {
  const user = await buildUserWithPassword("login-password");
  for (const serialized of [user.toObject(), user.toJSON()]) {
    for (const field of SECRET_FIELDS) {
      assert.ok(!(field in serialized), `${field} leaked via login serialization`);
    }
  }
  // Non-secret fields must remain intact (guard against over-stripping)
  const json = user.toJSON();
  assert.equal(json.email, "audit-case@example.com");
  assert.equal(json.isVerified, true);
  assert.equal(json.rank, "citizen");
});

test("password verification and JWT signing keep working after transform", async () => {
  const user = await buildUserWithPassword("correct-password");
  assert.equal(await user.checkPassword("correct-password"), true);
  assert.notEqual(await user.checkPassword("wrong-password"), true);

  const token = user.getJWT();
  const decoded = jwt.verify(token, process.env.JWT_SECRET_KEY);
  assert.equal(decoded._id, user.id);
});
