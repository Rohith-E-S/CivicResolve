// Verifies getComplaint populates the reporter with the public projection
// only (no address/lastLocation). The Complaint model is mocked, so this runs
// fully offline and inspects the real controller's populate arguments.
import test from "node:test";
import assert from "node:assert/strict";
import Complaint from "../models/complaint.model.js";

let populateCalls = [];
const fakeComplaint = {
  _id: "complaint-1",
  user: {
    _id: "reporter-1",
    fullName: "Reporter Fixture",
    email: "reporter@example.com",
    profilePic: "",
    address: "SECRET-STREET-ADDRESS",
    lastLocation: { coordinates: [76.0, 31.0] },
  },
  status: "new",
};

const stubFindOne = (t, result = fakeComplaint) => {
  t.mock.method(Complaint, "findOne", (filter) => {
    assert.deepEqual(filter, { _id: "complaint-1", isDeleted: { $ne: true } });
    return {
      populate(...args) {
        populateCalls.push(args);
        return Promise.resolve(result);
      },
    };
  });
};

const { getComplaint } = await import("../controllers/complaint.controller.js");

const makeReqRes = (isAdmin) => {
  const res = {
    code: 0,
    payload: null,
    status(c) {
      this.code = c;
      return this;
    },
    json(p) {
      this.payload = p;
      return this;
    },
  };
  return [{ params: { id: "complaint-1" }, user: { _id: "viewer-1", isAdmin } }, res];
};

test("getComplaint populates reporter with public fields only (non-admin viewer)", async (t) => {
  populateCalls = [];
  stubFindOne(t);
  const [req, res] = makeReqRes(false);
  await getComplaint(req, res);

  assert.equal(res.code, 201);
  assert.equal(populateCalls.length, 1, "expected exactly one populate call");
  const [field, projection] = populateCalls[0];
  assert.equal(field, "user");
  assert.match(projection, /fullName/);
  assert.doesNotMatch(projection, /email|homeDistrict/);
  assert.match(projection, /profilePic/);
  assert.doesNotMatch(projection, /address|lastLocation|password|resetPassword|fcmToken/,
    "private or auth fields must not appear in the populate projection");
  assert.equal(res.payload.complaint, fakeComplaint);
});

test("getComplaint uses the same public projection for admins", async (t) => {
  populateCalls = [];
  stubFindOne(t);
  const [req, res] = makeReqRes(true);
  await getComplaint(req, res);
  const [, projection] = populateCalls[0];
  assert.doesNotMatch(projection, /address|lastLocation/);
  assert.equal(res.payload.isAdmin, true);
});
