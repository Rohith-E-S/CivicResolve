import test from "node:test";
import assert from "node:assert/strict";
import API from "../src/api/axios.js";
import { googleLoginPayload, signupPayload, passwordError } from "../src/utils/authContracts.js";

// Adapters intercept requests locally: no HTTP, Google, email or database access.
const capture = async (url, data) => {
  let request;
  await API.post(url, data, { adapter: async (config) => {
    request = config;
    return { data: {}, status: 200, statusText: "OK", headers: {}, config };
  } });
  return request;
};

test("Google accepts only the opaque credential, never decoded identity", async () => {
  const payload = googleLoginPayload({ credential: "test-id-token", email: "ignored@example.test" });
  assert.deepEqual(payload, { credential: "test-id-token" });
  assert.throws(() => googleLoginPayload({}), /credential/);
  const request = await capture("/auth/google-login", payload);
  assert.equal(request.data, JSON.stringify(payload));
  assert.equal(request.headers.getContentType(), "application/json");
  assert.equal(request.withCredentials, true);
});

test("account creation binds pending email to a required signup proof", () => {
  const pending = { fullName: "Local Test", email: "local@example.test", password: "12345678", address: "Local" };
  assert.deepEqual(signupPayload(pending, pending.email, "test-signup-proof"), { ...pending, signupToken: "test-signup-proof" });
  assert.throws(() => signupPayload(pending, pending.email, undefined), /verification expired/);
  assert.throws(() => signupPayload(pending, "other@example.test", "proof"), /session expired/);
  assert.throws(() => signupPayload(null, pending.email, "proof"), /session expired/);
});

test("password policy checks minimum length and bcrypt's UTF-8 byte ceiling", () => {
  assert.match(passwordError("1234567"), /at least 8/);
  assert.equal(passwordError("12345678"), "");
  assert.equal(passwordError("a".repeat(72)), "");
  assert.match(passwordError("a".repeat(73)), /72 UTF-8 bytes/);
  assert.equal(passwordError("é".repeat(36)), "");
  assert.match(passwordError("é".repeat(37)), /72 UTF-8 bytes/);
  assert.equal(passwordError("😀".repeat(18)), "");
  assert.match(passwordError("😀".repeat(19)), /72 UTF-8 bytes/);
});

test("profile FormData is not JSON serialized and keeps the image part", async () => {
  const data = new FormData();
  data.append("fullName", "Local Test");
  data.append("address", "Local");
  data.append("profilePic", new Blob(["local test image"], { type: "image/png" }), "profile.png");
  const request = await capture("/auth/update", data);
  assert.equal(request.data, data);
  assert.equal(request.data.get("profilePic").name, "profile.png");
  assert.notEqual(request.headers.getContentType(), "application/json");
  assert.equal(request.withCredentials, true);
});
