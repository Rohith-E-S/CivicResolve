import test from "node:test";
import assert from "node:assert/strict";
import User from "../models/user.model.js";
import { googleLogin } from "../controllers/user.controller.js";

const claims = () => ({ aud: "fixture-client", iss: "https://accounts.google.com", exp: String(Date.now() / 1000 + 300), email_verified: "true", email: "fixture@example.invalid", sub: "fixture-subject" });
const response = () => ({ statusCode: 200, cookies: [], status(code) { this.statusCode = code; return this; }, cookie(...args) { this.cookies.push(args); }, json(body) { this.body = body; return this; } });
function configure(t) {
  const old = { audience: process.env.GOOGLE_CLIENT_ID, secret: process.env.JWT_SECRET_KEY };
  process.env.GOOGLE_CLIENT_ID = "fixture-client";
  process.env.JWT_SECRET_KEY = "offline-fixture-signing-key-49708d83b9334658b469";
  t.after(() => {
    for (const [key, value] of [["GOOGLE_CLIENT_ID", old.audience], ["JWT_SECRET_KEY", old.secret]]) {
      if (value === undefined) delete process.env[key]; else process.env[key] = value;
    }
  });
}

test("Google login requires credential before account lookup", async (t) => {
  configure(t);
  const lookup = t.mock.method(User, "findOne", () => { throw new Error("Unexpected lookup"); });
  const network = t.mock.method(globalThis, "fetch", () => { throw new Error("Unexpected request"); });
  const res = response();
  await googleLogin({ body: {} }, res);
  assert.equal(res.statusCode, 401);
  assert.equal(lookup.mock.callCount(), 0);
  assert.equal(network.mock.callCount(), 0);
  assert.equal(res.cookies.length, 0);
});

for (const [name, change] of [
  ["missing audience configuration", () => { delete process.env.GOOGLE_CLIENT_ID; }],
  ["wrong audience", (value) => { value.aud = "another-client"; }],
  ["expired credential", (value) => { value.exp = "1"; }],
  ["unverified email", (value) => { value.email_verified = "false"; }],
]) {
  test(`Google login fails closed for ${name}`, async (t) => {
    configure(t);
    const value = claims(); change(value);
    t.mock.method(globalThis, "fetch", async () => ({ ok: true, json: async () => value }));
    const lookup = t.mock.method(User, "findOne", () => { throw new Error("Unexpected lookup"); });
    const res = response();
    await googleLogin({ body: { credential: "offline-fixture" } }, res);
    assert.equal(res.statusCode, 401);
    assert.equal(lookup.mock.callCount(), 0);
    assert.equal(res.cookies.length, 0);
  });
}

test("verified claims select account and issue session without password serialization", async (t) => {
  configure(t);
  t.mock.method(globalThis, "fetch", async () => ({ ok: true, json: async () => claims() }));
  t.mock.method(User, "findOne", (filter) => {
    assert.deepEqual(filter, { email: claims().email });
    return { select: async () => new User({ email: claims().email, fullName: "Fixture", password: "not-a-real-hash" }) };
  });
  const res = response();
  await googleLogin({ body: { credential: "offline-fixture" } }, res);
  assert.equal(res.statusCode, 200);
  assert.equal(res.cookies.length, 1);
  assert.equal(res.body.success, true);
  assert.ok(!("password" in res.body.user));
});
