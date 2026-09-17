// Exercise the real socket registration block with inert adapters. The server
// entrypoint itself is never imported: no listener, database or network starts.
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";
import * as chatRules from "../utils/chatAuth.js";

const source = readFileSync(new URL("../server.js", import.meta.url), "utf8");
const start = source.indexOf('io.on("connection",');
const end = source.indexOf("await connectDB();", start);
assert.ok(start >= 0 && end > start, "socket registration block must exist");

function harness(user, complaint, admins = []) {
  const handlers = new Map();
  const events = [];
  const writes = [];
  const seen = [];
  const reads = [];
  const joins = [];
  const io = {
    on(event, callback) { if (event === "connection") callback(socket); },
    to() { return { emit: (...args) => events.push(args) }; },
  };
  const socket = {
    user, id: "offline-socket",
    on: (event, callback) => handlers.set(event, callback),
    emit: (...args) => events.push(args),
    join(room) { joins.push(room); },
  };
  const query = () => ({
    then(resolve, reject) { return Promise.resolve(complaint).then(resolve, reject); },
    select() { return Promise.resolve(complaint); },
  });
  const context = {
    ...chatRules,
    console: { log() {}, error() {} },
    io, socket,
    Complaint: {
      findById(id) { reads.push(id); return query(); },
      findOne(filter) { reads.push(filter); return query(); },
    },
    User: { find: () => ({ select: async () => admins }) },
    Message: {
      async create(value) { writes.push(value); return { _id: "saved" }; },
      findById() {
        return { populate() { return { populate: async () => ({ _id: "saved" }) }; } };
      },
      async updateMany(filter) { seen.push(filter); },
    },
    notifyAdminComment: async () => {},
    notifyAdminNewMessage: async () => {},
  };
  vm.runInNewContext(source.slice(start, end), context);
  return { handlers, writes, seen, events, reads, joins };
}

const ownerId = "aaaaaaaaaaaaaaaaaaaaaaaa";
const complaintId = "bbbbbbbbbbbbbbbbbbbbbbbb";
const complaint = { _id: complaintId, user: ownerId, city: "fixture", category: "other" };

test("socket handlers reject unrelated users before writing or broadcasting seen state", async () => {
  const h = harness({ _id: "cccccccccccccccccccccccc", isAdmin: false }, complaint);
  await h.handlers.get("sendMessage")({ complaintId, message: "Offline fixture" });
  await h.handlers.get("markSeen")({ complaintId });
  assert.equal(h.writes.length, 0);
  assert.equal(h.seen.length, 0);
  assert.ok(h.events.some(([event]) => event === "error"));
  assert.ok(h.events.every(([event]) => event !== "messagesSeen" && event !== "newMessage"));
});

test("socket handlers reject soft-deleted complaints even for admins", async () => {
  const h = harness({ _id: ownerId, isAdmin: true }, { ...complaint, isDeleted: true });
  await h.handlers.get("sendMessage")({ complaintId, message: "Offline fixture" });
  await h.handlers.get("markSeen")({ complaintId });
  assert.equal(h.writes.length, 0);
  assert.equal(h.seen.length, 0);
});

test("admin message uses server-derived recipient and normalized text", async () => {
  const h = harness({ _id: "dddddddddddddddddddddddd", fullName: "Fixture admin", isAdmin: true }, complaint);
  await h.handlers.get("sendMessage")({ complaintId, message: "  Offline fixture  " });
  assert.equal(h.writes.length, 1);
  assert.equal(h.writes[0].toUser, ownerId);
  assert.equal(h.writes[0].message, "Offline fixture");
  assert.ok(h.events.some(([event]) => event === "newMessage"));
});


test("invalid socket data and IDs fail before any database read", async () => {
  const h = harness({ _id: ownerId }, complaint);
  for (const value of [null, undefined, [], {}, 42, "bad-id", { $ne: null }]) {
    await h.handlers.get("joinComplaint")(value);
    await h.handlers.get("sendMessage")({ complaintId: value, message: "text" });
    await h.handlers.get("markSeen")({ complaintId: value });
  }
  await h.handlers.get("sendMessage")(null);
  await h.handlers.get("markSeen")(undefined);
  assert.equal(h.reads.length, 0); assert.equal(h.writes.length, 0); assert.equal(h.seen.length, 0);
});

test("owner sends to district admin, joins and marks only own incoming messages seen", async () => {
  const adminId = "dddddddddddddddddddddddd";
  const h = harness({ _id: ownerId, fullName: "Owner" }, complaint, [{ _id: adminId, homeDistrict: "fixture" }]);
  await h.handlers.get("joinComplaint")(complaintId.toUpperCase());
  await h.handlers.get("sendMessage")({ complaintId, message: "text", toUser: ownerId });
  await h.handlers.get("markSeen")({ complaintId });
  assert.equal(h.joins[0], complaintId);
  assert.equal(h.writes[0].toUser, adminId);
  assert.equal(h.seen[0].toUser, ownerId);
});

test("no admins means no write even with a client recipient", async () => {
  const h = harness({ _id: ownerId }, complaint);
  await h.handlers.get("sendMessage")({ complaintId, message: "text", toUser: ownerId });
  assert.equal(h.writes.length, 0);
  assert.ok(h.events.some(([event, data]) => event === "error" && data.message.includes("No recipient")));
});

test("admin may read orphaned complaint but cannot message a missing owner", async () => {
  const h = harness({ _id: ownerId, isAdmin: true }, { ...complaint, user: null });
  await h.handlers.get("sendMessage")({ complaintId, message: "text", toUser: ownerId });
  assert.equal(h.writes.length, 0);
});
