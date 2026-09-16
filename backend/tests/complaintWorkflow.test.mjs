import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import Complaint from "../models/complaint.model.js";
import User from "../models/user.model.js";
import Notification from "../models/notification.model.js";
import cloudinary from "../config/cloudinary.js";
import { transporter } from "../config/email.js";
import { updateComplaint, updateAfterImageUrl, updateComplaintStatus, rateComplaint } from "../controllers/complaint.controller.js";

function setup(t, { status = "in_progress", image = null } = {}) {
  const notifications = [], events = [], emails = [], queries = [];
  const doc = { _id: "complaint", user: "owner", status, afterImageUrl: image, timestamps: {},
    city: "city", category: "other", description: "issue", save: t.mock.fn(async () => {}) };
  t.mock.method(Complaint, "findOne", () => ({
    populate(path, fields) {
      assert.equal(path, "user"); assert.equal(fields, "fullName email");
      doc.user = { _id: "owner", fullName: "Owner", email: "owner@example.test" };
      return Promise.resolve(doc);
    }, then(resolve, reject) { return Promise.resolve(doc).then(resolve, reject); }
  }));
  t.mock.method(Complaint, "findById", () => ({ populate(path, fields) {
    assert.equal(path, "user"); assert.equal(fields, "fullName profilePic isAdmin civicPoints rank");
    return Promise.resolve({ ...doc, user: { _id: "owner", fullName: "Owner" } });
  } }));
  t.mock.method(User, "find", query => { queries.push(query); return { select: async () => [{ _id: "neighbor" }] }; });
  t.mock.method(Notification, "create", async data => { notifications.push(data); return data; });
  t.mock.method(transporter, "sendMail", async data => { emails.push(data); });
  const upload = t.mock.method(cloudinary.uploader, "upload", async () => ({ secure_url: "https://example.test/after" }));
  t.mock.method(fs, "unlinkSync", () => {});
  const io = { sockets: { adapter: { rooms: new Map() } },
    emit(event, data) { events.push({ event, data }); },
    to(room) { return { emit(event, data) { events.push({ room, event, data }); } }; } };
  const req = { params: { id: doc._id }, body: {}, user: { _id: "admin", isAdmin: true }, app: { get: () => io } };
  const res = { code: 200, status(code) { this.code = code; return this; }, json(body) { this.body = body; return this; } };
  return { doc, req, res, notifications, events, emails, queries, upload };
}

for (const [name, handler, withImage] of [
  ["combined upload", updateComplaint, true], ["after image upload", updateAfterImageUrl, true],
  ["combined status", updateComplaint, false], ["status", updateComplaintStatus, false],
]) {
  test(`${name} starts pending verification with matching timestamps and notifications`, async t => {
    const s = setup(t, { image: withImage ? null : "https://example.test/existing" });
    s.req.body.status = "resolved";
    if (withImage) s.req.file = { path: "/tmp/mock-image" };
    await handler(s.req, s.res);
    assert.ok(s.res.code < 300, JSON.stringify(s.res.body));
    assert.equal(s.doc.status, "pending_verification");
    assert.ok(s.doc.timestamps.pendingVerification instanceof Date);
    assert.equal(s.doc.timestamps.resolved, undefined);
    assert.equal(s.doc.save.mock.callCount(), 1);
    assert.equal(s.res.body.complaint.status, "pending_verification");
    assert.equal(s.res.body.complaint.user.email, undefined);
    assert.ok(s.events.some(e => e.event === "statusUpdated" && e.data.status === "PENDING_VERIFICATION"));
    assert.ok(!s.events.some(e => e.event === "statusUpdated" && e.data.status === "RESOLVED"));
    assert.ok(s.notifications.some(n => n.type === "status_changed" && n.metadata.oldStatus === "in_progress" && n.metadata.newStatus === "pending_verification"));
    assert.ok(s.notifications.some(n => n.type === "verification_needed" && n.userId === "neighbor"));
    assert.ok(s.queries.every(q => q._id.$ne === "owner"));
    assert.ok(!s.emails.some(e => e.subject.includes("Has Been Resolved")));
  });
}
for (const handler of [updateComplaint, updateComplaintStatus]) {
  for (const status of ["resolved", "confirmed_resolved", "disputed"]) {
    test(`${handler.name} rejects ${status} without bypassing verification`, async t => {
      const s = setup(t); s.req.body.status = status;
      await handler(s.req, s.res);
      assert.ok(s.res.code >= 400); assert.equal(s.doc.save.mock.callCount(), 0);
      assert.equal(s.doc.status, "in_progress"); assert.equal(s.notifications.length, 0);
    });
  }
}
test("combined upload cannot smuggle a final-state status alongside an image", async t => {
  const s = setup(t); s.req.body.status = "confirmed_resolved"; s.req.file = { path: "/tmp/mock" };
  await updateComplaint(s.req, s.res);
  assert.equal(s.res.code, 400); assert.equal(s.upload.mock.callCount(), 0); assert.equal(s.doc.save.mock.callCount(), 0);
});
test("combined ordinary update records inProgress timestamp", async t => {
  const s = setup(t, { status: "under_review" }); s.req.body.status = "IN_PROGRESS";
  await updateComplaint(s.req, s.res);
  assert.equal(s.res.code, 200); assert.ok(s.doc.timestamps.inProgress instanceof Date);
});
test("owner may rate confirmed_resolved complaints", async t => {
  const s = setup(t, { status: "confirmed_resolved" }); s.req.user = { _id: "owner" }; s.req.body.rating = 5;
  await rateComplaint(s.req, s.res);
  assert.equal(s.res.code, 200); assert.equal(s.doc.rating, 5);
});
