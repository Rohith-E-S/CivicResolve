import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import Complaint from "../models/complaint.model.js";
import User from "../models/user.model.js";
import Message from "../models/message.model.js";
import Notification from "../models/notification.model.js";
import cloudinary from "../config/cloudinary.js";
import * as handlers from "../controllers/complaint.controller.js";

const fields = "fullName profilePic isAdmin civicPoints rank";
const reporter = { _id: "reporter", fullName: "Reporter", profilePic: "", isAdmin: false,
  civicPoints: 5, rank: "citizen", email: "private@example.test", address: "private",
  lastLocation: { coordinates: [1, 2] }, fcmToken: "private", password: "private" };
const safeReporter = Object.fromEntries(["_id", ...fields.split(" ")].map(k => [k, reporter[k]]));
const request = () => ({ params: { id: "complaint" }, query: { city: "city", state: "state" },
  body: { latitude: 31, longitude: 76 }, user: { _id: "viewer", isAdmin: true, createdAt: "2020-01-01" },
  app: { get: () => null } });
const response = () => ({ code: 200, status(code) { this.code = code; return this; },
  json(body) { this.body = body; return this; } });
function setup(t, status = "pending_verification") {
  const doc = { _id: "complaint", user: "reporter", status, city: "city", category: "other",
    location: { coordinates: [76, 31] }, verifications: [], verificationCount: 1,
    timestamps: {}, save: async () => {}, dispute: { userId: "viewer" } };
  let populates = 0;
  const chain = (value) => ({ sort() { return this; }, skip() { return this; }, limit() { return this; },
    populate(path, projection) {
      assert.equal(path, "user"); assert.equal(projection, fields); populates++;
      const project = d => ({ ...d, user: safeReporter });
      return Promise.resolve(Array.isArray(value) ? value.map(project) : project(value));
    }, then(resolve, reject) { return Promise.resolve(value).then(resolve, reject); } });
  t.mock.method(Complaint, "findOne", () => chain(doc));
  t.mock.method(Complaint, "findById", () => chain(doc));
  t.mock.method(Complaint, "findOneAndUpdate", async () => doc);
  t.mock.method(Complaint, "find", () => chain([doc]));
  t.mock.method(Complaint, "create", async () => doc);
  t.mock.method(Complaint, "countDocuments", async () => 1);
  t.mock.method(Message, "distinct", async () => [doc._id]);
  t.mock.method(User, "find", () => ({ select: async () => [] }));
  t.mock.method(User, "findById", () => ({ select: async () => null }));
  t.mock.method(Notification, "create", async data => data);
  t.mock.method(cloudinary.uploader, "upload", async () => ({ secure_url: "https://example.test/proof" }));
  t.mock.method(fs, "unlinkSync", () => {});
  return { doc, populates: () => populates };
}

for (const name of ["createComplaint", "getMyComplaint", "getAllComplaints", "filterComplaintOnStateCity",
  "getPaginatedComplaints", "getMyPaginatedComplaints", "getComplaintsWithMessages", "getMyComplaintsWithMessages",
  "verifyComplaint", "disputeComplaint", "resolveDispute", "rateComplaint"]) {
  test(`${name} returns only public reporter fields`, async t => {
    const state = setup(t, name === "resolveDispute" ? "disputed" : name === "rateComplaint" ? "resolved" : "pending_verification");
    const req = request(); const res = response();
    req.body = { latitude: 31, longitude: 76, city: "city", state: "state",
      landmark: "spot", description: "issue", category: "other", action: "confirm", rating: 4 };
    if (name === "disputeComplaint") req.file = { path: "/tmp/mock-proof" };
    await handlers[name](req, res);
    assert.ok(res.code < 300, JSON.stringify(res.body));
    assert.equal(state.populates(), 1);
    const complaint = res.body.complaint || res.body.complaints?.[0] || res.body.newComplaint?.[0];
    if (complaint) assert.deepEqual(complaint.user, safeReporter);
  });
}

test("admin stats bucket entries populate Android user objects, not IDs", async t => {
  const state = setup(t, "new"); const req = request(); const res = response();
  await handlers.getComplaintStats(req, res);
  assert.equal(res.code, 200); assert.equal(state.populates(), 1);
  assert.deepEqual(res.body.stats.newComplaint[0].user, safeReporter);
  assert.deepEqual(res.body.stats.inProgressComplaint, []);
  assert.deepEqual(res.body.stats.resolvedComplaint, []);
});
