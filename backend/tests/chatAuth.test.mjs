// Offline unit tests for the shared chat authorization rule used by the
// socket handlers in server.js. Pure functions only — no DB, no sockets.
import test from "node:test";
import assert from "node:assert/strict";
import { canAccessComplaintChat, normalizeChatMessage, MAX_CHAT_MESSAGE_LENGTH } from "../utils/chatAuth.js";

const owner = { _id: "aaaaaaaaaaaaaaaaaaaaaaaa" };
const otherUser = { _id: "bbbbbbbbbbbbbbbbbbbbbbbb" };
const admin = { _id: "cccccccccccccccccccccccc", isAdmin: true };
const complaint = { _id: "dddddddddddddddddddddddd", user: owner._id, isDeleted: false };
const deletedComplaint = { ...complaint, isDeleted: true };

test("complaint owner can access chat", () => {
  assert.equal(canAccessComplaintChat(owner, complaint), true);
});

test("admin can access any complaint chat", () => {
  assert.equal(canAccessComplaintChat(admin, complaint), true);
});

test("unrelated user is denied", () => {
  assert.equal(canAccessComplaintChat(otherUser, complaint), false);
});

test("soft-deleted complaints deny even the owner", () => {
  assert.equal(canAccessComplaintChat(owner, deletedComplaint), false);
  assert.equal(canAccessComplaintChat(admin, deletedComplaint), false);
});

test("missing user or complaint is denied", () => {
  assert.equal(canAccessComplaintChat(null, complaint), false);
  assert.equal(canAccessComplaintChat(owner, null), false);
});

test("ObjectId-like wrappers compare by string value", () => {
  const oidOwner = { _id: { toHexString: () => "aaaaaaaaaaaaaaaaaaaaaaaa" } };
  const oidComplaint = { user: { toHexString: () => "aaaaaaaaaaaaaaaaaaaaaaaa" } };
  assert.equal(canAccessComplaintChat(oidOwner, oidComplaint), true);
});

test("normalizeChatMessage trims and accepts valid text", () => {
  assert.deepEqual(normalizeChatMessage("  pothole near gate 3  "), { message: "pothole near gate 3" });
});

test("normalizeChatMessage rejects empty, non-string, and oversized input", () => {
  assert.ok(normalizeChatMessage("   ").error);
  assert.ok(normalizeChatMessage(42).error);
  assert.ok(normalizeChatMessage("x".repeat(MAX_CHAT_MESSAGE_LENGTH + 1)).error);
  assert.ok(!normalizeChatMessage("x".repeat(MAX_CHAT_MESSAGE_LENGTH)).error);
});

test("missing and malformed identities never match by accident", () => {
  for (const id of [undefined, null, "", "bad-id", {}, 42]) {
    assert.equal(canAccessComplaintChat({ _id: id }, { user: id }), false);
    assert.equal(canAccessComplaintChat({ _id: id, isAdmin: true }, complaint), false);
  }
  assert.equal(canAccessComplaintChat({ ...otherUser, isAdmin: "true" }, complaint), false);
  assert.equal(canAccessComplaintChat(owner, { user: { _id: owner._id } }), true);
});
