# Android session and auth audit fixes

## Contracts

- Cookie persistence uses encrypted preferences, storing original response URL, exact expiry, and OkHttp Set-Cookie representation. Domain/host-only, path, Secure, HttpOnly, persistent/session semantics supported by OkHttp are retained. Expired/deleted cookies are pruned; requests use `Cookie.matches`. Unsupported browser attributes such as SameSite are not exposed by OkHttp 4.12's Cookie API.
- Legacy bare tokens are discarded rather than migrated without trustworthy scope. Users upgrading from that storage must sign in again.
- JSON token fallback is host-only, Secure, HttpOnly, path `/`, bound to the configured backend URL. An identical server cookie is preserved, including expiry; fallback tokens use session-cookie semantics and server JWT expiry/revocation remains authoritative.
- OTP verification must return a nonempty `signupToken`. Only the pending signup for the same email receives it, in memory, then create-account sends it. Successful signup/logout clears pending plaintext/proof. Process recreation requires restarting signup; proof/password are not persisted or placed in navigation arguments.
- Signup/reset send plaintext over TLS, without hashing/trimming, with minimum 8 UTF-16 characters (matching backend JS length) and maximum 72 UTF-8 bytes. Existing shorter login passwords remain accepted by the client.
- Google request model is `{credential}` only. `play-services-auth:21.0.0` exists, but no client ID or sign-in launcher is configured. Fake Google buttons/callbacks are removed; email/password-reset alternatives are explained visibly. This change does not claim to implement OAuth or add dependencies.
- Admin stats retain the existing `User?` model. Offline fixtures match backend's populated public reporter object (or null when deleted), with private fields and unused stats arrays omitted.

## Reproduce offline validation

From this branch, with the existing cached SDK/dependencies:

```sh
cd /tmp/civicresolve-android/android
JAVA_HOME=/opt/android-studio/jbr ANDROID_HOME=/home/rohith/Android/Sdk \
  ./gradlew --offline :app:testDebugUnitTest :app:assembleDebug
```

Gradle 9.4.1 selected cached JDK 21 from its daemon criteria; Android compile SDK 36.1. No local.properties, secrets, new libraries, production requests, or service startup required.

Result: **12 tests passed**, zero failures (6 cookie, 3 auth/password, 2 stats, 1 existing arithmetic test), debug APK built. Existing Android/Compose deprecation warnings remain.

## Manual checks not executed

On a non-production configured backend and test device: encrypted preferences persistence/restart/upgrade, OTP signup success and expired-proof retry, process recreation during signup, password reset, logout, socket revocation, and admin stats rendering. No emulator/device or live OAuth identity was used. There is deliberately no runnable Google sign-in action until configuration and a real credential launcher are provided and verified. Backend auth and complaint PRs must deploy with the coordinated contracts.
