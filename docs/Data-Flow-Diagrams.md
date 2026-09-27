# CivicResolve — Data Flow Sequence Diagrams

How **every bit of data** moves through the codebase: web (React), Android (Kotlin), Express API, Socket.IO, MongoDB, Cloudinary, Gmail, Google OAuth, Nominatim and ngrok.

> [!tip] Viewing this in Obsidian
> Mermaid is built into Obsidian — just open this note and switch to **Reading view** or **Live Preview**. Every diagram is self-contained, so you can also copy any single block into a scratch note.

**System map (orientation)**

```mermaid
flowchart LR
    subgraph Clients
        W[Web React Vite]
        A[Android Kotlin Compose]
    end
    subgraph Edge
        V[Vite dev proxy]
        N[ngrok tunnel]
    end
    subgraph Server[Node Express + Socket.IO]
        R[REST routes]
        S[Socket handlers]
        C[Controllers]
        SV[Services<br/>notifications + points]
    end
    subgraph Data
        DB[(MongoDB<br/>problemRegPortal)]
        CL[Cloudinary<br/>images + AI tagging]
    end
    subgraph External
        GM[Gmail SMTP]
        GO[Google OAuth]
        NM[Nominatim OSM]
    end
    W --> V --> R
    A --> N --> R
    W & A <--ws--> S
    R --> C --> DB
    S --> C
    C --> CL & GM & DB
    C <--verify--> GO
    W & A --> NM
    C --> SV --> DB
```

**Contents**

- [[#0 · The full complaint lifecycle (master diagram)]]
- [[#1 · Authentication — OTP, Google OAuth, session, password reset]]
- [[#2 · The location pipeline — GPS, Nominatim, home district, heartbeat]]
- [[#3 · Filing a complaint — duplicate check, geo-fence, AI categorization]]
- [[#4 · Admin triage & resolution — after photo, fan-out, email, points]]
- [[#5 · Real-time chat — Socket.IO rooms, recipient resolution, seen receipts]]
- [[#6 · Community verification — 3 nearby verifiers, atomic threshold flip]]
- [[#7 · Disputes — AI photo check, reopen vs confirm]]
- [[#8 · Notifications & gamification plumbing]]
- [[#9 · Read paths — public feed, map, analytics]]
- [[#Participant legend]]

---

## 0 · The full complaint lifecycle (master diagram)

The backbone of the whole system — one complaint's life from report to confirmed resolution. Each phase is zoomed into by diagrams 1–9.

```mermaid
sequenceDiagram
    autonumber
    actor CT as Citizen
    actor AD as Admin
    participant CL as Web + Android clients
    participant API as Express REST
    participant WS as Socket.IO server
    participant DB as MongoDB
    participant EXT as Cloudinary Gmail Google Nominatim

    rect rgb(230 240 255)
    Note over CT,EXT: 1 · Onboarding — diagram 1 + 2
    CT->>CL: sign up with OTP or Google
    CL->>API: auth + district onboarding
    API->>EXT: OTP email · Google token check · reverse geocode
    API->>DB: User with homeDistrict + JWT cookie
    end

    rect rgb(255 244 230)
    Note over CT,EXT: 2 · Report — diagram 3
    CT->>CL: photo + GPS + description
    CL->>API: POST create-complaint
    API->>EXT: Cloudinary upload + AI tags
    API->>DB: Complaint status new
    API->>WS: notify neighbors + admins
    end

    rect rgb(240 230 255)
    Note over CT,EXT: 3 · Triage + chat — diagrams 4 + 5
    AD->>API: status changes · after photo · chat
    API->>DB: status resolved · Message docs
    API->>WS: statusUpdated · newMessage · new_notification
    end

    rect rgb(232 255 235)
    Note over CT,EXT: 4 · Verification — diagram 6
    CT->>API: POST verify with GPS
    API->>DB: 3 verifiers within 500 m → resolved
    API->>WS: points_earned · rank_up
    end

    rect rgb(255 232 232)
    Note over CT,EXT: 5 · Optional dispute — diagram 7
    CT->>API: dispute with fresh photo
    API->>EXT: Cloudinary AI evidence check
    AD->>API: reopen or confirm
    end
```

---

## 1 · Authentication — OTP, Google OAuth, session, password reset

**Code:** `backend/controllers/user.controller.js` · `backend/middleware/auth.middleware.js` · `frontend/src/pages/Signup.jsx` · `frontend/src/components/OnboardingDistrict.jsx`

```mermaid
sequenceDiagram
    autonumber
    actor U as Citizen
    participant FE as React Signup + OtpVerify pages
    participant API as Express /api/auth
    participant DB as MongoDB users + otp
    participant GM as Gmail via Nodemailer
    participant GO as Google tokeninfo

    rect rgb(230 240 255)
    Note over U,GM: OTP signup path
    U->>FE: email + password
    FE->>API: POST send-otp
    API->>DB: User.findOne email (block existing)
    API->>DB: OTP.deleteMany + create 6-digit code
    Note right of DB: TTL index auto-purges expired codes
    API->>GM: sendMail HTML OTP
    GM-->>U: OTP email
    U->>FE: enter code
    FE->>API: POST verify-otp
    API->>DB: match latest + deleteOne (consume)
    FE->>API: POST create-account
    API->>DB: bcrypt hash + User.create isVerified
    API-->>FE: set token cookie (24h JWT) + user JSON
    FE->>FE: mirror to localStorage → navigate /dashboard
    end

    rect rgb(235 255 235)
    Note over U,GO: Google OAuth path
    U->>FE: click GoogleLogin widget
    FE->>FE: @react-oauth/google ID token
    FE->>API: POST google-login id_token
    API->>GO: GET oauth2.googleapis.com/tokeninfo
    GO-->>API: payload (aud checked vs GOOGLE_CLIENT_ID)
    API->>DB: find or create isGoogleUser
    API-->>FE: token cookie (7-day JWT)
    Note over FE: Android Google login is stubbed in this build
    end

    rect rgb(255 244 230)
    Note over U,DB: Password reset path
    FE->>API: POST sendPasswordResetOtp
    API->>GM: reset OTP email
    FE->>API: POST verifyPasswordResetOtp → one-time token
    FE->>API: POST reset-password/:id token + new password
    API->>DB: hash + clear resetToken + invalidate old JWT
    end

    rect rgb(245 240 255)
    Note over U,DB: Session checks on every page load
    FE->>API: GET check-auth
    API->>DB: protectRoute reads token cookie → req.user
    API-->>FE: 200 user or 401 → redirect /login
    end
```

> [!note] Transport detail
> The JWT always lives in an http-only `token` cookie. Web keeps a **UI-only** mirror in localStorage; Android persists it via `EncryptedSharedPreferences` inside `CookieJarImpl` and re-injects the cookie on every Retrofit call.

---

## 2 · The location pipeline — GPS, Nominatim, home district, heartbeat

Location data feeds three features: the **geo-fence** for reporting, the **admin jurisdiction** routing, and the **1 km verification fan-out**.

**Code:** `frontend/src/components/OnboardingDistrict.jsx` · `frontend/src/pages/Explore.jsx` · `android/.../utils/LocationUtilsNew.kt` + `LocationUtils.kt` · `backend/controllers/user.controller.js` (updateHomeDistrict, updateUserLocation)

```mermaid
sequenceDiagram
    autonumber
    actor U as Citizen
    participant CL as Web or Android client
    participant GPS as Browser geo or FusedLocation
    participant NM as Nominatim OSM
    participant API as Express /api/auth
    participant DB as MongoDB User

    rect rgb(230 240 255)
    Note over U,DB: One-time district onboarding
    CL->>GPS: get current position
    GPS-->>CL: lat lng
    CL->>NM: GET reverse geocode lat lng
    NM-->>CL: city state address
    U->>CL: confirm detected district
    CL->>API: POST update-home-district
    API->>DB: save User.homeDistrict
    Note right of DB: becomes reporting geo-fence<br/>+ admin routing key
    end

    rect rgb(232 255 235)
    Note over U,DB: Continuous heartbeat (Explore + Dashboard)
    loop while app open
        CL->>GPS: position update
        CL->>API: POST update-location lat lng
        API->>DB: User.lastLocation GeoJSON Point<br/>+ lastLocationUpdatedAt
    end
    Note right of DB: 2dsphere index + 24h freshness window<br/>power the $nearSphere 1km verification fan-out
    end
```

---

## 3 · Filing a complaint — duplicate check, geo-fence, AI categorization

**Code:** `frontend/src/components/dashboard/NewComplaint.jsx` · `backend/middleware/uploads.js` · `backend/controllers/complaint.controller.js` (createComplaint:127, getNearbyComplaints:1459, analyzeImage:49) · `backend/config/constants.js` · `backend/services/notificationService.js`

```mermaid
sequenceDiagram
    autonumber
    actor U as Citizen
    participant FE as NewComplaint.jsx
    participant MP as MapPicker pin
    participant NM as Nominatim OSM
    participant API as Express /api/complaint
    participant MU as multer uploads/
    participant CL as Cloudinary
    participant DB as MongoDB Complaint
    participant WS as Socket.IO
    participant NB as Neighbors + district admins

    rect rgb(230 240 255)
    Note over U,NM: Location + duplicate pre-check
    U->>FE: pick location (GPS or drag pin)
    FE->>MP: pin coordinates
    MP-->>FE: lat lng
    FE->>NM: GET reverse geocode
    NM-->>FE: city state landmark autofill
    FE->>API: GET nearby?lat lng radius=500
    API->>DB: $nearSphere on 2dsphere index<br/>+ haversine distanceMeters
    DB-->>API: active complaints within radius
    API-->>FE: nearby duplicates list
    alt duplicates found
        U->>FE: "Submit anyway"
    end
    end

    rect rgb(255 244 230)
    Note over U,DB: Create with AI categorization
    U->>FE: description + category + photo
    FE->>API: POST create-complaint (multipart imageUrl)
    API->>MU: image-only 10MB check → UUID temp file
    MU-->>API: req.file path
    API->>API: geo-fence — city/state/landmark<br/>fuzzy match vs homeDistrict
    alt out of district
        API-->>FE: 403 Out of Bounds
    end
    API->>CL: upload with categorization google_tagging
    CL-->>API: tags + confidence
    API->>API: hasKeyword maps tags → one of 10 categories
    API->>DB: Complaint.create<br/>GeoJSON location [lng lat]<br/>status new · timestamps.reported
    API->>MU: unlink temp file
    API-->>FE: populated complaint JSON
    end

    rect rgb(232 255 235)
    Note over API,NB: Async side-effects
    API->>WS: notifyNewInDistrict (matching homeDistrict)
    API->>WS: notifyAdminNewReport (district + global admins)
    WS->>DB: Notification.create × N
    WS-->>NB: emit new_notification to each user room
    Note over NB: Android renders system notification<br/>via always-on socket
    end
```

> [!note] Where the "AI" lives
> There is no external LLM. Categorization = **Cloudinary's google_tagging add-on**; tags are mapped to civic categories via the keyword lists in `backend/config/constants.js`. The same trick is reused for dispute evidence in diagram 7. A standalone `POST analyze-image` endpoint lets the client preview the detected category before submitting.

---

## 4 · Admin triage & resolution — after photo, fan-out, email, points

**Code:** `frontend/src/components/adminDashboard/*` · `backend/controllers/complaint.controller.js` (updateComplaint:787, updateComplaintStatus:496, updateAfterImageUrl:650) · `backend/services/pointsService.js` · `backend/config/email.js`

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant AD as AdminDashboard React or Android
    participant API as Express /api/complaint
    participant CL as Cloudinary
    participant DB as MongoDB
    participant WS as Socket.IO
    participant GM as Nodemailer Gmail
    participant PS as pointsService
    participant NB as Nearby citizens
    actor R as Reporter citizen

    rect rgb(230 240 255)
    Note over A,DB: Load the queue
    AD->>API: GET admin/stats + admin/list (page status search)
    API->>DB: escaped-regex search + pagination<br/>isDeleted != true
    DB-->>AD: stats + complaints
    end

    rect rgb(255 244 230)
    Note over A,GM: Resolve with after photo
    A->>AD: set status + attach after photo
    AD->>API: POST update-complaint-status-upload-image/:id
    API->>CL: upload after image
    API->>DB: save afterImageUrl<br/>file present → status resolved
    API->>WS: statusUpdated → complaint room
    API->>WS: globalToast → city-wide announcement
    API->>DB: Notification statusChanged → reporter
    WS-->>R: new_notification
    end

    rect rgb(240 230 255)
    Note over API,GM: Verification fan-out
    API->>DB: $nearSphere User.lastLocation ≤ 1km<br/>lastLocationUpdatedAt within 24h
    alt no fresh nearby users
        API->>DB: fallback — homeDistrict regex match
    end
    API->>WS: notifyNearbyForVerification × each
    WS-->>NB: new_notification verification_needed
    API->>GM: resolution email with after photo → reporter
    end

    rect rgb(232 255 235)
    Note over API,R: Reward the reporter
    API->>PS: awardPoints reporter report_resolved 10
    PS->>DB: civicPoints update + updateRank<br/>(citizen → neighborhood_watch →<br/>community_hero → district_guardian → civic_champion)
    PS-->>R: socket points_earned + rank_up
    API-->>AD: updated complaint
    end
```

> [!warning] Status coercion rule
> Text-only `updateComplaintStatus` with status `resolved` but **no photo** is coerced to `pending_verification` — a photo is mandatory for a true resolve. `upload-after-image/:id` performs the same fan-out on its own.

---

## 5 · Real-time chat — Socket.IO rooms, recipient resolution, seen receipts

**Code:** `backend/server.js:62-244` · `backend/middleware/socket.auth.js` · `backend/routes/message.route.js` · `frontend/src/components/ChatBox.jsx` · `frontend/src/pages/ComplaintChat.jsx` · `android/.../ChatViewModel` + `ChatBox.kt`

```mermaid
sequenceDiagram
    autonumber
    actor C as Citizen
    actor A as Admin
    participant FE as ChatBox web or Android
    participant API as Express REST
    participant WS as Socket.IO server.js
    participant SA as socketAuth middleware
    participant DB as MongoDB
    participant NS as notificationService

    rect rgb(230 240 255)
    Note over C,DB: Connect + history
    C->>FE: open complaint chat
    FE->>API: GET /api/messages/:complaintId
    Note right of API: ACL owner-or-admin<br/>populated chat history
    API-->>FE: chat history
    FE->>WS: connect with auth.token JWT
    WS->>SA: verify token → socket.user
    C->>FE: join rooms
    FE->>WS: join_room (personal notification room)
    FE->>WS: joinComplaint complaintId
    WS->>DB: ACL — owner or admin only
    WS->>WS: socket.join complaintId
    A->>WS: joinComplaint same room
    end

    rect rgb(255 244 230)
    Note over C,NS: Send a message
    C->>FE: type + send
    FE->>WS: sendMessage complaintId text
    WS->>DB: load complaint
    alt sender is citizen
        WS->>DB: find admins → district fuzzy-match on city<br/>fallback any admin
        WS->>DB: Message.create toUser district admin
    else sender is admin
        WS->>DB: Message.create toUser complaint owner
    end
    WS->>DB: populate fromUser toUser
    WS-->>FE: newMessage to complaint room (both sides)
    WS->>NS: notifyAdminNewMessage × matched admins<br/>or notifyAdminComment → owner
    NS->>DB: Notification.create
    NS-->>A: new_notification
    end

    rect rgb(232 255 235)
    Note over C,DB: Seen receipts
    A->>WS: markSeen complaintId
    WS->>DB: updateMany hasSeen true<br/>where toUser = me
    WS-->>FE: messagesSeen seenBy → both UIs flip receipts
    end
```

> [!note] Recipient resolution is server-side
> Clients never choose `toUser`. The server resolves admin→owner, or citizen→district-matched admin (`"all"` / empty homeDistrict = global admin, fuzzy substring + first-word core match), with a fallback to all admins so messages are never lost — `backend/server.js:100-214`.

---

## 6 · Community verification — 3 nearby verifiers, atomic threshold flip

**Code:** `backend/controllers/complaint.controller.js:1692` · `backend/services/pointsService.js` · `frontend/src/pages/ComplaintOverviewPage.jsx` · `android/.../ComplaintOverviewScreen`

```mermaid
sequenceDiagram
    autonumber
    actor V as Nearby citizen
    participant CL as client with GPS
    participant API as verifyComplaint
    participant DB as MongoDB Complaint
    participant PS as pointsService
    participant WS as Socket.IO
    actor R as Reporter

    rect rgb(230 240 255)
    Note over V,DB: Eligibility gauntlet
    V->>CL: tap Verify on pending_verification issue
    CL->>API: POST verify/:id {latitude longitude}
    API->>DB: load complaint
    API->>API: guard — account age ≥ 7 days
    API->>API: guard — not already in verifications[]
    API->>API: haversine GPS vs complaint.location<br/>must be ≤ 500 m
    end

    rect rgb(232 255 235)
    Note over V,PS: Atomic verification + rewards
    API->>DB: findOneAndUpdate aggregation pipeline<br/>$concatArrays push + $size recount<br/>(race-proof — no double count)
    alt verificationCount ≥ 3
        API->>DB: atomic flip status resolved<br/>+ timestamps.resolved
        API->>WS: statusUpdated → room + globalToast
    end
    API->>PS: awardPoints verifier verified_issue 5
    PS->>DB: civicPoints + updateRank
    PS-->>V: socket points_earned (+ rank_up if promoted)
    end

    rect rgb(255 244 230)
    Note over API,R: Fan-out
    API->>WS: verified_by_you → verifier
    API->>WS: verified_by_peer → reporter
    API->>WS: admin_verification_alert → all admins
    opt threshold reached
        API->>WS: notifyOwnerVerified + statusUpdated
    end
    API-->>CL: updated complaint
    end
```

> [!tip] Gamification triggers elsewhere
> `supportComplaint` (:1518) awards the reporter **3 pts** on the *first-ever* upvote per user (`upvotersAwarded[]` anti-farming), and the 3-verification flip awards the reporter **10 pts** via `report_resolved`.

---

## 7 · Disputes — AI photo check, reopen vs confirm

**Code:** `backend/controllers/complaint.controller.js:1837` (disputeComplaint) and `:1941` (resolveDispute)

```mermaid
sequenceDiagram
    autonumber
    actor D as Nearby citizen
    participant CL as client
    participant API as Express /api/complaint
    participant CLD as Cloudinary
    participant DB as MongoDB
    participant WS as Socket.IO
    actor A as Admin

    rect rgb(255 232 232)
    Note over D,DB: Raise a dispute
    D->>CL: "Issue still there" + fresh photo
    CL->>API: POST dispute/:id (multipart disputePhoto)
    API->>API: guards — ≤ 500 m away<br/>+ never verified this complaint
    API->>CLD: upload with google_tagging
    CLD-->>API: tags + confidence
    API->>API: build dispute.aiAnalysis<br/>issueStillPresent from tag presence<br/>confidence 85 or 10 + reasoning
    API->>DB: save dispute{userId photo aiAnalysis}<br/>status disputed · timestamps.disputed
    API->>WS: statusUpdated → room
    API->>WS: notifyAdminDisputed → all admins
    end

    rect rgb(240 230 255)
    Note over D,A: Admin verdict
    A->>API: PATCH dispute/resolve/:id {action}
    alt action = reopen
        API->>DB: status re_opened<br/>reset verifications[] and count<br/>clear afterImageUrl
        API->>DB: awardPoints disputer dispute_accepted 20
        WS-->>D: points_earned
    else action = confirm
        API->>DB: status confirmed_resolved
        API->>WS: notifyDisputeResolved → reporter + admins
    end
    API->>WS: statusUpdated → room
    API-->>CL: updated complaint
    end
```

---

## 8 · Notifications & gamification plumbing

Every notify call follows the same two-step pattern: **DB write + socket emit**. No FCM — Android push is an always-on Socket.IO connection.

**Code:** `backend/services/notificationService.js` · `backend/services/pointsService.js` · `android/.../notification/NotificationSocketManager.kt` · `backend/routes/notification.route.js` (REST endpoints exist but no UI consumer)

```mermaid
sequenceDiagram
    autonumber
    participant T as Trigger<br/>(controller or socket handler)
    participant NS as notificationService
    participant DB as MongoDB
    participant WS as Socket.IO
    participant W as Web React
    participant AN as Android socket manager
    participant PS as pointsService

    rect rgb(230 240 255)
    Note over T,AN: Notification path (13 types)
    T->>NS: notifySomething(io, payload)
    NS->>DB: Notification.create<br/>(userId type title message complaintId)
    NS->>WS: io.to(userId).emit new_notification
    WS-->>W: live in-app toast + badge
    WS-->>AN: new_notification
    AN->>AN: Android NotificationManager → system push
    end

    rect rgb(232 255 235)
    Note over T,PS: Points path
    T->>PS: awardPoints(userId, reason)
    PS->>DB: points table by reason<br/>verified_issue 5 · report_upvoted 3<br/>report_resolved 10 · dispute_accepted 20
    PS->>DB: updateRank across 5 rank thresholds
    PS-->>W: socket points_earned
    opt threshold crossed
        PS-->>W: socket rank_up
    end
    end
```

---

## 9 · Read paths — public feed, map, analytics

**Code:** `frontend/src/pages/LandingPage.jsx` + `Explore.jsx` + `MapView.jsx` · `frontend/src/components/explore/*` · `backend/controllers/analytics.controller.js` + complaint.controller.js (getPublicFeed:1605, getPublicStats:1651) · `android/.../ExploreScreen` + `MapViewScreen`

```mermaid
sequenceDiagram
    autonumber
    actor V as Visitor or citizen
    participant FE as Web or Android UI
    participant API as Express /api/complaint
    participant DB as MongoDB aggregations
    participant OS as OSM + CARTO tiles

    rect rgb(230 240 255)
    Note over V,DB: Public landing stats — no auth
    V->>FE: open landing page
    FE->>API: GET public-stats?district=all
    API->>DB: count + group by status/category
    DB-->>FE: totals
    end

    rect rgb(232 255 235)
    Note over V,OS: Explore feed + map
    V->>FE: browse Explore or Map
    FE->>API: GET feed?district&page&limit
    API->>DB: district regex over city/state/landmark<br/>paginated minimal user populate
    DB-->>FE: complaint cards
    FE->>OS: fetch map tiles
    Note over FE: Leaflet + CARTO (web)<br/>osmdroid voyager/dark (Android)
    end

    rect rgb(240 230 255)
    Note over V,DB: Dashboards + analytics
    V->>FE: open Dashboard or AdminDashboard
    FE->>API: GET analytics/user · analytics/admin · admin/stats · my-stats
    API->>DB: weekly $dateToString trends<br/>category + district breakdowns<br/>top reporters + resolution-time buckets<br/>rank percentile
    DB-->>FE: aggregation JSON
    FE->>FE: Recharts (web) or Vico (Android)
    end
```

> [!warning] Dormant code worth knowing
> `backend/routes/analytics.route.js` is defined but **never mounted** (analytics live under the complaint router). The `/api/notifications` REST endpoints have no UI consumer — all notification delivery rides the socket. `frontend/public/themes/` are static design mocks.

---

## Participant legend

| Participant | Actual component | Key files |
|---|---|---|
| Web client | React 19 + Vite 7 SPA (citizen + admin) | `frontend/src/App.jsx`, `api/axios.js`, `utils/socket.js` |
| Android client | Kotlin + Jetpack Compose + Retrofit/Moshi | `android/.../di/AppContainer.kt`, `data/remote/ApiService.kt` |
| Express REST | Express 5, port 4000 | `backend/server.js`, `routes/*.route.js` |
| Socket.IO | Same Node process, room-based | `backend/server.js`, `middleware/socket.auth.js` |
| MongoDB | DB `problemRegPortal`, 5 collections | `backend/models/*.model.js` |
| Cloudinary | Image hosting + google_tagging "AI" | `backend/config/cloudinary.js`, `config/constants.js` |
| Gmail / Nodemailer | OTP + resolution emails | `backend/config/email.js` |
| Google OAuth | ID token verification | `user.controller.js:280` |
| Nominatim / OSM | Reverse geocoding + map tiles | `NewComplaint.jsx`, `LocationUtilsNew.kt` |
| ngrok | Public tunnel for Android → dev backend | `backend/scripts/start-ngrok.mjs`, `docker-entrypoint.mjs` |
| pointsService | Civic points + 5 ranks | `backend/services/pointsService.js` |
| notificationService | 13 notification types, DB + socket | `backend/services/notificationService.js` |

**Topologies:** web dev → Vite (5173) proxies `/api` + `/socket.io` to Express (4000); Android → ngrok tunnel (default in `app/build.gradle.kts`, overridable with `-PapiBaseUrl=`).
