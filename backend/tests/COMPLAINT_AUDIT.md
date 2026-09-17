# Complaint/chat audit regression checks

Scope: audit #3 (reporter privacy), #6 (admin resolution workflow), #9
(Android admin stats user objects), and complaint-chat authorization boundaries.

From the repository root, with the existing backend dependencies installed:

```sh
node --test backend/tests/*.test.mjs
node --check backend/server.js
node --check backend/controllers/complaint.controller.js
```

No database, listener, Cloudinary request, email delivery or `.env` is needed.
The controller suites call the actual exported handlers and replace model and
upload/mail methods with inert fixtures. Socket tests execute the actual
connection-registration block with VM adapters; the server entrypoint is not
started. Pure chat rules additionally cover malformed/missing identities.

Contract changes:
- Complaint response reporters expose `_id`, `fullName`, `profilePic`, `isAdmin`,
  `civicPoints`, and `rank` only. Internal combined-update email lookup remains
  separate and is tested; email content now requests verification, not closure.
- Combined image/status updates and after-image uploads enter
  `pending_verification`, timestamp that stage and notify owner/neighbors. A
  status-only `resolved` request requires an after image and also enters this
  stage. Ordinary updates cannot select `confirmed_resolved` or `disputed`.
- Ratings accept both `resolved` and `confirmed_resolved`.
- Admin stats bucket entries populate the same safe reporter object as detail
  responses. Count-only citizen stats remain count-only.
- Chat requires valid IDs and owner/admin access to active complaints. Message
  recipients are server-selected; if no admin/owner recipient is available, no
  message is saved. The existing 2,000-character message bound remains enforced.

Session expiry/revocation is owned by the separate auth change. This branch
preserves the existing `socketAuth` middleware and authenticated `socket.user`
contract. Tests here do not claim live database, transport or Android UI coverage.
