# Auth & Session Hardening

Security hardening for `backend` authentication and session handling. Contract changes are **breaking for legacy clients**; web, Android, and chat integrations are aligned via the shared team contract.

## Client contract (final)
- `POST /api/auth/send-otp` `{ email }` — unchanged response; 60s cooldown per account/purpose, visible failure if delivery fails.
- `POST /api/auth/verify-otp` `{ email, otp }` → `{ success, message, signupToken }`. `signupToken` is a 64-char opaque hex proof, single-use, 10-minute expiry; the server stores only its SHA-256 hash.
- `POST /api/auth/create-account` `{ fullName, email, password, address?, signupToken }` — requires the exact same email and the single-use `signupToken` from step 2. On success: session cookie + `{ success, token, user, message }`.
- `POST /api/auth/login` unchanged keys. Android HTTP may also send `Authorization: Bearer <token>`.
- `POST /api/auth/google-login` `{ credential }` only — a Google ID token verified against `GOOGLE_CLIENT_ID` (aud/iss/exp enforced, fails closed).
- Password policy (signup/reset): minimum 8 characters, maximum 72 UTF-8 bytes (bcrypt limit). Login accepts existing (shorter) passwords, rejects invalid types/operators/overlong values.
- Session cookie `token`: `HttpOnly; SameSite=Lax; Path=/`; `Secure` when `NODE_ENV=production`. JSON `token` preserved for Android.
- Logout (`POST /api/auth/logout`, now authenticated) and password reset revoke **all** sessions of that user (persisted session version) and disconnect sockets. Every socket packet rechecks JWT expiry and version; idle sockets are disconnected at JWT expiry.
- Sockets: browser handshake uses cookie credentials (`withCredentials: true`); Android may still use `handshake.auth.token`. Unauthenticated/tampered sockets are rejected at handshake; revoked sessions disconnect mid-session.

## Deployment requirements
- `JWT_SECRET_KEY` **must** be a fresh random secret of at least 32 bytes (e.g. `openssl rand -hex 32`). Weak, default, or missing secrets now **fail closed** at signing/verification — the API will not start sessions without one.
- `GOOGLE_CLIENT_ID` must be set to the exact OAuth client ID(s); Google logins fail closed without it. Rotate credentials and the JWT secret on any suspicion of compromise; rotation revokes all existing sessions.
- Rate limiting is stored in MongoDB (`authratelimits` collection) — limits are shared across workers and restarts.
- Before rollout, verify the unique OTP `(email, isForgotPassword)` and AuthGrant `(email, purpose)` indexes and TTL indexes are built successfully. Existing duplicate OTP records may block index creation: drain/invalidate pending OTPs and resolve duplicates during maintenance. No database migration was performed here.
- Existing JWTs lack session-version claims and are intentionally rejected; users must sign in again. Legacy pending OTPs and reset links must be reissued. Coordinate deployment with Android #73 and web #75; complaint/chat changes are #72.
- `GOOGLE_CLIENT_ID` accepts a single client ID, not a comma-separated list; match web's `VITE_GOOGLE_OAUTH_CLIENT_ID`. Use same-site HTTPS with credentialed proxy/socket forwarding.
- Configure trusted proxies deliberately for per-IP throttling. With the default configuration, users behind a reverse proxy may share its quota; never blindly trust forwarded headers.
- Rollback must coordinate backend and clients. Code reverts do not rotate credentials or remove database indexes. No credentials were rotated and no production services were contacted.