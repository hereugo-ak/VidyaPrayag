# Notification System — Exhaustive Specification

> **Status**: Foundation built, push-bridge incomplete, logout token cleanup missing, channels/sounds/custom-views/deep-link-routing not yet implemented.
>
> **Last updated**: 2026-06-27 (rev 4 — filled all 9 implementation gaps: NavGraphV2 deep-link routing, NotificationScheduler full Exposed queries, permission callback fix, merged displayNotification(), preferences API contracts + routing, AuthApi.logout() signature, clearSession() JS/wasm fix, preferences filtering in Notify.toUsers(), pull-to-refresh wiring)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Current State — What Exists](#2-current-state--what-exists)
3. [The Critical Gap — In-App ↔ Push Bridge](#3-the-critical-gap--in-app--push-bridge)
4. [Device Token Registration Audit — Per Role](#4-device-token-registration-audit--per-role)
5. [Exhaustive Notification Catalog](#5-exhaustive-notification-catalog)
6. [Backend Changes Required](#6-backend-changes-required)
7. [Database Changes Required](#7-database-changes-required)
8. [Android Client Implementation](#8-android-client-implementation)
9. [iOS Client Implementation](#9-ios-client-implementation)
10. [Notification Channels](#10-notification-channels)
11. [Notification Sounds](#11-notification-sounds)
12. [Custom Notification Views](#12-custom-notification-views)
13. [Deep-Link Routing on Tap](#13-deep-link-routing-on-tap)
14. [Notification Action Buttons](#14-notification-action-buttons)
15. [Notification Grouping / Stacking](#15-notification-grouping--stacking)
16. [Scheduled Notifications](#16-scheduled-notifications)
17. [Notification Preferences Screen](#17-notification-preferences-screen)
18. [Corner Cases & Edge Scenarios](#18-corner-cases--edge-scenarios)
19. [Implementation Roadmap](#19-implementation-roadmap)
20. [API Contracts](#20-api-contracts)
21. [Testing Requirements](#21-testing-requirements)
22. [Sequence Diagrams](#22-sequence-diagrams)
23. [Configuration](#23-configuration)
24. [Operational Requirements](#24-operational-requirements)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SERVER (Ktor)                               │
│                                                                     │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────────────────┐  │
│  │  Trigger     │───▶│  Notify.kt   │───▶│  NotificationsTable  │  │
│  │  (attendance,│    │  (DB write)  │    │  (in-app inbox)       │  │
│  │   marks, …)  │    └──────┬───────┘    └───────────────────────┘  │
│  │              │           │                                        │
│  │              │           │  ◀── THE MISSING BRIDGE               │
│  │              │           ▼                                        │
│  │              │    ┌──────────────┐    ┌───────────────────────┐  │
│  │              │    │ Notification │───▶│  Firebase Admin SDK   │  │
│  │              │    │ Service      │    │  (FCM sendEachFor     │  │
│  │              │    │ (FCM push)   │    │   Multicast)          │  │
│  │              │    └──────┬───────┘    └───────────┬───────────┘  │
│  └──────────────┘           │                        │              │
│                             │                        │              │
│  ┌──────────────────────────┴────────────────────────┘              │
│  │  DeviceTokenRepository  ◀── POST /api/device-tokens              │
│  │  (device_tokens table)                                        │
│  └─────────────────────────────────────────────────────────────────┘
│                                                                     │
│  API Endpoints:                                                     │
│    GET   /api/v1/notifications          (inbox list, role-aware)    │
│    GET   /api/v1/notifications/summary  (unread count for bell)     │
│    PATCH /api/v1/notifications/{id}/read                            │
│    POST  /api/v1/notifications/read-all                             │
│    POST  /api/device-tokens              (FCM token registration)   │
│    POST  /api/admin/notifications/send   (admin broadcast)          │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ FCM push (hybrid: notification + data)
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      ANDROID CLIENT                                 │
│                                                                     │
│  ┌──────────────────────┐    ┌──────────────────────────────────┐   │
│  │ VidyaPrayagFirebase  │───▶│ NotificationManagerHelper        │   │
│  │ MessagingService     │    │  - channel creation              │   │
│  │ (FCM receiver)       │    │  - system-tray display           │   │
│  └──────────────────────┘    │  - deep-link PendingIntent       │   │
│                               └──────────────────────────────────┘   │
│  ┌──────────────────────┐    ┌──────────────────────────────────┐   │
│  │ DeviceTokenRegistrar │    │ NotificationsScreenV2            │   │
│  │ (token fetch →       │    │ (in-app inbox UI)                │   │
│  │  compare → register) │    │ NotificationsViewModel           │   │
│  └──────────────────────┘    └──────────────────────────────────┘   │
│                                                                     │
│  AndroidManifest.xml:                                               │
│    - POST_NOTIFICATIONS permission                                  │
│    - FirebaseMessagingService registered                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Current State — What Exists

### 2.1 In-App Notification Spine (DB-backed)

| Component | File | Status |
|---|---|---|
| DB table | `server/.../db/Tables.kt:1214` `NotificationsTable` | ✅ Built |
| Write path | `server/.../feature/notifications/Notify.kt` | ✅ Built |
| Recipient resolvers | `server/.../feature/notifications/NotifyRecipients.kt` | ✅ Built |
| Inbox API | `server/.../feature/notifications/NotificationsRouting.kt` | ✅ Built |
| Client API | `shared/.../feature/parent/data/remote/ParentApi.kt:57` | ✅ Built |
| Client ViewModel | `shared/.../feature/parent/presentation/NotificationsViewModel.kt` | ✅ Built |
| Inbox UI | `composeApp/.../ui/v2/screens/notifications/NotificationsScreenV2.kt` | ✅ Built |
| Bell + unread dot | Parent, Teacher, School portal headers | ✅ Built |

### 2.2 FCM Push Delivery (System Tray)

| Component | File | Status |
|---|---|---|
| Firebase Admin init | `server/.../feature/notification/firebase/FirebaseAdminInitializer.kt` | ✅ Built |
| FCM dispatch service | `server/.../feature/notification/service/NotificationService.kt` | ✅ Built |
| Device token repo | `server/.../feature/notification/repository/DeviceTokenRepository.kt` | ✅ Built |
| Token registration API | `server/.../feature/notification/api/NotificationRouting.kt` | ✅ Built |
| Admin broadcast | `POST /api/admin/notifications/send` | ✅ Built |
| FCM receiver | `composeApp/.../notification/VidyaPrayagFirebaseMessagingService.kt` | ✅ Built |
| System tray helper | `composeApp/.../notification/NotificationManagerHelper.kt` | ✅ Built |
| Token registrar | `shared/.../notification/DeviceTokenRegistrar.kt` | ✅ Built |
| Permission flow | `shared/.../notification/AndroidNotificationService.kt` | ✅ Built |
| Permission VM | `shared/.../presentation/PermissionViewModel.kt` | ✅ Built |
| Channel creation | `NotificationManagerHelper.createDefaultChannel()` | ✅ Single channel only |
| Manifest registration | `composeApp/src/androidMain/AndroidManifest.xml:41-47` | ✅ Built |
| App init | `composeApp/.../EnRollPlusApp.kt:19` | ✅ Built |

### 2.3 What Does NOT Exist

| Missing | Impact |
|---|---|
| **Notify → NotificationService bridge** | In-app notifications never trigger FCM push |
| **Per-category channels** | Only one `general_notifications` channel; users can't customize per category |
| **Notification sounds** | No `.setSound()`, no raw sound resources |
| **Custom notification layouts** | Default system layout only; no branded cards |
| **Deep-link routing in MainActivity** | Tap opens app but doesn't navigate to the target screen |
| **Notification action buttons** | No "Approve", "Mark read", "View" buttons on notifications |
| **Notification grouping** | Each notification gets a unique ID; no stacking |
| **Scheduled notifications** | No cron/scheduler for fee reminders, calendar reminders |
| **Notification preferences screen** | Footer button is a no-op `clickable {}` |
| **iOS notification support** | No `IOSNotificationService` implementation |

---

## 3. The Critical Gap — In-App ↔ Push Bridge

### The Problem

`Notify.toUsers()` writes rows to `notifications` table (the in-app inbox). It does **NOT** call `NotificationService.send()` to push via FCM. So when a teacher marks attendance, the parent sees it in their inbox **only if they open the app and pull the notification list** — they never get a system tray push.

The only way FCM push is triggered today is via the admin broadcast endpoint (`POST /api/admin/notifications/send`), which is a manual testing tool.

### The Fix

In `server/.../feature/notifications/Notify.kt`, after the `batchInsert`, also call `NotificationService.send()` with the same title/body/deepLink/category. The `NotificationService` will resolve device tokens for the recipients and fan out via FCM.

**Key file to modify**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notifications/Notify.kt`

**Approach**:
```
Notify.toUsers() 
  → batchInsert into NotificationsTable (existing)
  → NotificationService.send(SendNotificationRequest(...))  (NEW)
    → resolve device tokens via DeviceTokenRepository
    → FCM multicast (hybrid payload)
    → reconcile invalid tokens
```

**Corner cases**:
- `NotificationService.send()` must be best-effort (wrapped in `runCatching`) so a push failure never fails the originating HTTP request
- When Firebase credentials are absent, `send()` returns `success=false, sent=0` — this is already handled gracefully
- Recipients with no registered device tokens get the in-app row but no push — correct behavior
- The `userIds` passed to `Notify.toUsers()` are `UUID` — `NotificationService.send()` expects `List<String>`, so map them

---

## 4. Device Token Registration Audit — Per Role

### 4.1 Registration Flow (All Roles)

The token registration flow is **role-agnostic** — the same path runs for parent, teacher, school admin, and super admin:

```
User logs in → MainViewModel.userToken becomes non-null
  → notificationService.syncDeviceToken()
    → AndroidNotificationService.syncDeviceToken(force=false)
      → DeviceTokenRegistrar.syncRegistration(context, force=false)
        1. Check FirebaseApp.getApps(context).isEmpty() → skip if no Firebase
        2. Fetch FCM token via FirebaseMessaging.getInstance().token
        3. Resolve PreferenceRepository + NotificationApi from Koin
        4. Check prefs.getUserToken() → skip if not authenticated
        5. Compare fetched token with prefs.getFcmToken() (cached)
        6. If unchanged AND not forced → skip (network no-op)
        7. POST /api/device-tokens { token, platform, appVersion, deviceModel }
        8. On success → cache token in prefs.setFcmToken(token)
```

**Second entry point** — FCM token rotation:
```
Firebase rotates token → VidyaPrayagFirebaseMessagingService.onNewToken(token)
  → DeviceTokenRegistrar.syncRegistration(context)  (same flow as above)
```

### 4.2 Per-Role Verification

| Role | Token Sync Trigger | schoolId Resolution | Verdict |
|---|---|---|---|
| **Parent** | `MainViewModel` sees `userToken != null` → `syncDeviceToken()` | Server resolves from `app_users.school_id` (may be null for unlinked parent) | ✅ Works |
| **Teacher** | Same — `MainViewModel` is role-agnostic | Server resolves from `app_users.school_id` | ✅ Works |
| **School Admin** | Same | Server resolves from `app_users.school_id` | ✅ Works |
| **Super Admin** | Same | Server resolves from `app_users.school_id` (may be null) | ✅ Works |

### 4.3 Bugs Found in Token Registration

#### BUG 1: Logout does NOT clear cached FCM token — re-login with different account silently fails to re-register

**Root cause**: `AuthRepositoryImpl.logout()` calls `preferenceRepository.clearSession()` which clears `userToken`, `userRole`, `refreshToken` — but does **NOT** clear the cached FCM token (`prefs.getFcmToken()`).

**Impact**: 
1. User A logs out on a shared device.
2. User B logs in on the same device.
3. `MainViewModel` fires `syncDeviceToken()` → `DeviceTokenRegistrar.syncRegistration()`.
4. `DeviceTokenRegistrar` fetches the FCM token (same as before — FCM tokens don't rotate on logout).
5. Compares with cached token → **they match** → skips registration.
6. The device token in the DB still points to User A (`device_tokens.user_id = User A`).
7. **User B never receives push notifications.** User A continues receiving them on a device they no longer use.

**Fix** (client-side): In `PreferenceRepository.clearSession()` (or in `AuthRepositoryImpl.logout()`), also clear the FCM token cache:
```kotlin
// In PreferenceRepository.clearSession() or AuthRepositoryImpl.logout():
preferenceRepository.setFcmToken(null)
```

**Fix** (server-side): The server `POST /logout` endpoint should also deactivate the caller's device tokens. But the server doesn't know which specific token to deactivate (it only sees the JWT, not the FCM token). Options:
- **Option A**: Client sends its FCM token in the logout request body → server calls `deactivateToken(token)`.
- **Option B**: Server calls `deactivateAllForUser(uid)` — but this kills ALL the user's devices, not just the one logging out. **Only acceptable if the user has one device.**
- **Recommended**: Option A — add `fcmToken` to `LogoutDto`.

#### BUG 2: Duplicate device-token registration endpoint

There are **two** endpoints that register device tokens:

| Endpoint | File | Used by client? | Has schoolId resolution? | Has appVersion/deviceModel? |
|---|---|---|---|---|
| `POST /api/device-tokens` | `NotificationRouting.kt:68` | ✅ Yes (`NotificationApi.kt:51`) | ✅ Yes (`resolveSchoolIdForUser`) | ✅ Yes |
| `POST /api/v1/notifications/device-token` | `NotificationsRouting.kt:252` | ❌ No | ❌ No (raw insert) | ❌ No |

The second endpoint (`/api/v1/notifications/device-token`) is a simpler, redundant version that:
- Does NOT resolve `schoolId` from `app_users` — leaves it null
- Does NOT record `appVersion` or `deviceModel`
- Does NOT use `DeviceTokenRepository` — inlines the insert/update logic
- Is not called by any client code

**Fix**: Remove the `POST /api/v1/notifications/device-token` endpoint from `NotificationsRouting.kt:252-282`. The canonical endpoint is `POST /api/device-tokens` in `NotificationRouting.kt`.

#### BUG 3: `EnRollPlusApp.onCreate()` does not call `syncRegistration()`

The `DeviceTokenRegistrar.kt` comment says registration happens from two entry points: `Application.onCreate()` and `onNewToken()`. But `EnRollPlusApp.onCreate()` only calls `NotificationManagerHelper.createDefaultChannel(this)` — it does NOT call `DeviceTokenRegistrar.syncRegistration()`.

**Impact**: Token sync only happens when `MainViewModel` is created (which happens when `App` composable runs). This is usually fine, but if `MainViewModel` is delayed (e.g. Koin not fully initialized), there's a window where the token isn't registered.

**Fix**: Add `DeviceTokenRegistrar.syncRegistration()` call in `EnRollPlusApp.onCreate()` after `initKoin`:
```kotlin
override fun onCreate() {
    super.onCreate()
    instance = this
    NotificationManagerHelper.createDefaultChannel(this)
    initKoin { androidContext(this@EnRollPlusApp) }
    // Best-effort early token sync — MainViewModel will also sync, but this
    // covers the case where the user is already authenticated and the token
    // hasn't been registered yet.
    CoroutineScope(Dispatchers.IO).launch {
        runCatching { DeviceTokenRegistrar.syncRegistration(this@EnRollPlusApp) }
    }
}
```

**Note**: This is a minor optimization, not a critical bug — `MainViewModel` does trigger sync reliably.

#### BUG 4: `requestPermission()` callback is never wired

`AndroidNotificationService.requestPermission()` calls `ActivityCompat.requestPermissions()` but the result callback (`onRequestPermissionsResult`) is never wired in `MainActivity`. The `onResult` callback parameter is never invoked.

**Impact**: The permission dialog appears, but the app never knows whether the user granted or denied. The `PermissionViewModel` relies on `hasPermission()` polling on next launch, which works — but the immediate callback path is dead.

**Fix**: Either:
- **Option A**: Wire `onRequestPermissionsResult` in `MainActivity` and relay the result to a shared state holder (e.g. a `MutableStateFlow<Boolean>` in `AndroidNotificationService`).
- **Option B**: Use `ActivityResultContracts.RequestPermission()` via `rememberLauncherForActivityResult` in the Compose layer (recommended — more idiomatic for Compose).

---

## 5. Exhaustive Notification Catalog

### 5.1 Notifications Already Triggered (17 call sites in 13 files)

#### Attendance — Student Absent/Late

| Field | Value |
|---|---|
| **Trigger** | Teacher marks student absent or late in attendance |
| **Trigger file** | `server/.../feature/teacher/TeacherAttendanceRouting.kt:401` |
| **Category** | `attendance` |
| **Recipients** | Parents of the student (via `NotifyRecipients.parentsOfStudent`) |
| **Title** | `"Attendance update"` |
| **Body** | `"Your child was marked absent in Class-Section · Subject on 2026-06-27."` |
| **Deep link** | `parent/academics/attendance` |
| **Ref type** | `attendance` |
| **Push priority** | High (time-sensitive — parent should know immediately) |
| **Sound** | Distinct alert tone (not default) |
| **Channel** | `attendance_alerts` |
| **Corner cases** | Multiple children in same class → each parent gets one notification. Teacher edits attendance back to present → no "un-absent" notification (by design). Bulk mark → one notification per absent student, not one per parent. |

#### Marks / Results Published

| Field | Value |
|---|---|
| **Trigger** | Teacher publishes assessment OR admin publishes results |
| **Trigger files** | `server/.../feature/teacher/TeacherGradebookRouting.kt:792`, `server/.../feature/school/ResultsRouting.kt:394` |
| **Category** | `marks` |
| **Recipients** | Parents of class (via `NotifyRecipients.parentsOfClass`) |
| **Title** | `"Results published"` |
| **Body** | `"Marks for \"Unit Test 1\" (Mathematics) have been published."` |
| **Deep link** | `parent/academics/marks` |
| **Ref type** | `assessment` |
| **Push priority** | High |
| **Sound** | Default notification tone |
| **Channel** | `academic_updates` |
| **Corner cases** | Unpublish → no re-notify (by design). Large class (100+ students) → batch fan-out, all parents at once. Teacher publishes then immediately unpublishes → parents who saw the push may see empty marks screen — acceptable. |

#### Homework Assigned

| Field | Value |
|---|---|
| **Trigger** | Teacher creates homework assignment |
| **Trigger file** | `server/.../feature/teacher/TeacherHomeworkRouting.kt:536` |
| **Category** | `homework` |
| **Recipients** | Parents of class (via `NotifyRecipients.parentsOfClass`) |
| **Title** | `"New homework"` |
| **Body** | `"Mathematics: Algebra Worksheet — due 2026-06-30."` |
| **Deep link** | `parent/academics` |
| **Ref type** | `homework` |
| **Push priority** | Default |
| **Sound** | Default notification tone |
| **Channel** | `academic_updates` |
| **Corner cases** | Homework edited after creation → no re-notify (by design). Homework deleted → notification remains in inbox (stale reference). Class with 0 students → `parentsOfClass` returns empty → `Notify.toUsers` no-ops. |

#### Announcement Posted

| Field | Value |
|---|---|
| **Trigger** | School admin posts announcement |
| **Trigger file** | `server/.../feature/announcements/AnnouncementRouting.kt:251` |
| **Category** | `announcement` |
| **Recipients** | Audience-scoped parents + teachers (via `NotifyRecipients.parentsForAudience` + `teachersInSchool`) |
| **Title** | Announcement title |
| **Body** | Subtitle or first 140 chars of description |
| **Deep link** | `announcements/{eventId}` |
| **Ref type** | `announcement` |
| **Push priority** | Default |
| **Sound** | Default notification tone |
| **Channel** | `announcements` |
| **Corner cases** | ALL_SCHOOL audience → every parent + every teacher (could be 1000+). CLASS-scoped → only parents of that grade. STUDENT-scoped → only that student's parents. CUSTOM audience → falls back to ALL_SCHOOL (no precise app_user mapping). Announcement edited → no re-notify. Announcement deleted → inbox notification becomes stale. |

#### Calendar Event Published

| Field | Value |
|---|---|
| **Trigger** | Admin publishes academic calendar event |
| **Trigger file** | `server/.../feature/calendar/AcademicCalendarRouting.kt:529` |
| **Category** | `calendar` |
| **Recipients** | Role-filtered: parents, students, and/or teachers based on `notifyStudents/notifyParents/notifyTeachers` flags |
| **Title** | Event title |
| **Body** | First 140 chars of description |
| **Deep link** | `calendar/{eventId}` |
| **Ref type** | `calendar_event` |
| **Push priority** | Default |
| **Sound** | Default notification tone |
| **Channel** | `calendar_events` |
| **Corner cases** | All three notify flags false → no notification. Event edited after publish → no re-notify. Event deleted → stale inbox reference. School with 0 active users → empty recipients → no-op. |

#### Leave Request — Parent Applied

| Field | Value |
|---|---|
| **Trigger** | Parent submits leave request for child |
| **Trigger file** | `server/.../feature/parent/ParentLeaveRouting.kt:189` |
| **Category** | `leave` |
| **Recipients** | Class teacher(s) assigned to the child's class |
| **Title** | `"New leave request"` |
| **Body** | `"$childName has a pending leave request for your review."` |
| **Deep link** | `teacher/leave-requests` |
| **Ref type** | `leave_request` |
| **Push priority** | High (teacher action required) |
| **Sound** | Default notification tone |
| **Channel** | `leave_requests` |
| **Corner cases** | No teacher assigned to class → request still lands in admin queue, but no teacher notification is sent. Multiple teachers for same class → each gets a notification. |

#### Leave Request — Decided by Teacher

| Field | Value |
|---|---|
| **Trigger** | Teacher approves/rejects parent leave request |
| **Trigger file** | `server/.../feature/teacher/TeacherLeaveRouting.kt:214` |
| **Category** | `leave` |
| **Recipients** | The parent who submitted the request |
| **Title** | `"Leave request approved"` / `"Leave request rejected"` |
| **Body** | `"$childName's leave request was approved by $teacherName."` |
| **Deep link** | `parent/leave` |
| **Ref type** | `leave_request` |
| **Push priority** | High |
| **Sound** | Default notification tone |
| **Channel** | `leave_requests` |
| **Corner cases** | Leave request not found → 404, no notification. Leave request not assigned to this teacher → 403, no notification. Already decided → 409, no notification. |

#### Leave Request — Decided by Admin

| Field | Value |
|---|---|
| **Trigger** | Admin overrides/approves/rejects leave request |
| **Trigger file** | `server/.../feature/school/LeaveRequestsRouting.kt:306` (`notifyLeaveDecision`) |
| **Category** | `leave` |
| **Recipients** | Varies — parent and/or teacher depending on context |
| **Title** | `"Leave request approved/rejected/updated"` |
| **Body** | `"$requesterName's leave request was $verb."` |
| **Deep link** | `parent/leave` (configurable) |
| **Ref type** | `leave_request` |
| **Push priority** | High |
| **Sound** | Default notification tone |
| **Channel** | `leave_requests` |
| **Corner cases** | Empty recipients → no-op. Duplicate recipients → `Notify.toUsers` deduplicates. |

#### Leave Request — Teacher Self-Leave

| Field | Value |
|---|---|
| **Trigger** | Teacher applies for their own leave |
| **Trigger file** | `server/.../feature/teacher/TeacherSelfLeaveRouting.kt:172` |
| **Category** | `leave` |
| **Recipients** | School admins (`school_admin` + `admin` roles) |
| **Title** | `"New staff leave request"` |
| **Body** | `"$teacherName applied for leave ($dateFrom → $dateTo)."` |
| **Deep link** | `school/leave-requests` |
| **Ref type** | `leave_request` |
| **Push priority** | High |
| **Sound** | Default notification tone |
| **Channel** | `leave_requests` |
| **Corner cases** | No admins in school → no notification (request still exists in DB). |

#### Child-Link Request — Parent Submitted

| Field | Value |
|---|---|
| **Trigger** | Parent requests to link to a child by roll number |
| **Trigger file** | `server/.../feature/parent/ParentLinkRouting.kt:610` |
| **Category** | `link_request` |
| **Recipients** | School admins (via `NotifyRecipients.adminsInSchool`) |
| **Title** | `"New child-link request"` |
| **Body** | `"A parent requested to link to roll $rollNumber."` |
| **Deep link** | `admin/link-requests` |
| **Ref type** | `link_request` |
| **Push priority** | High (admin action required) |
| **Sound** | Default notification tone |
| **Channel** | `link_requests` |
| **Corner cases** | Roll number ambiguous (multiple schools) → `ROLL_AMBIGUOUS` error, no notification. Phone mismatch → `NeedsReview` status, different notification body ("needs review"). Throttled (too many pending) → 429, no notification. No admins → notification created but nobody sees it (edge case). |

#### Child-Link Request — Approved

| Field | Value |
|---|---|
| **Trigger** | Admin approves parent-child link |
| **Trigger file** | `server/.../feature/parent/ParentLinkRouting.kt:842` |
| **Category** | `link_request` |
| **Recipients** | The parent |
| **Title** | `"Child link approved"` |
| **Body** | `"$childName has been linked to your account."` |
| **Deep link** | `parent/dashboard` |
| **Ref type** | `link_request` |
| **Push priority** | High |
| **Sound** | Positive/success tone |
| **Channel** | `link_requests` |
| **Corner cases** | Link already decided → 409, no notification. Link not found → 404. |

#### Child-Link Request — Rejected

| Field | Value |
|---|---|
| **Trigger** | Admin rejects parent-child link |
| **Trigger file** | `server/.../feature/parent/ParentLinkRouting.kt:895` |
| **Category** | `link_request` |
| **Recipients** | The parent |
| **Title** | `"Child link request declined"` |
| **Body** | `"Your request to link $childName was not approved. Please verify the roll number with the school."` |
| **Deep link** | `parent/dashboard` |
| **Ref type** | `link_request` |
| **Push priority** | High |
| **Sound** | Default notification tone |
| **Channel** | `link_requests` |
| **Corner cases** | Link already decided → 409. Link not found → 404. |

#### Message — Teacher Broadcast to Class

| Field | Value |
|---|---|
| **Trigger** | Teacher sends a message to all parents of a class |
| **Trigger file** | `server/.../feature/teacher/TeacherMessagesRouting.kt:325` |
| **Category** | `message` |
| **Recipients** | All parents of the class |
| **Title** | `"Message from $teacherName"` |
| **Body** | First 120 chars of message body |
| **Deep link** | `parent/messages` |
| **Ref type** | `message` |
| **Push priority** | Default |
| **Sound** | Message tone |
| **Channel** | `messages` |
| **Corner cases** | Class with 0 parents → no recipients → no-op. |

#### Message — 1:1 (Teacher/Admin → Parent)

| Field | Value |
|---|---|
| **Trigger** | Individual message sent via MessagingCore |
| **Trigger file** | `server/.../feature/school/MessagingCore.kt` |
| **Category** | `message` |
| **Recipients** | The specific parent |
| **Title** | `"New message from $senderName"` |
| **Body** | Message content (truncated) |
| **Deep link** | `parent/messages` |
| **Ref type** | `message` |
| **Push priority** | Default |
| **Sound** | Message tone |
| **Channel** | `messages` |
| **Corner cases** | Sender = recipient (self-message) → should be filtered. |

### 5.2 Notifications NOT Yet Implemented (To Add)

#### Fee Due Reminder

| Field | Value |
|---|---|
| **Trigger** | Scheduled job — fee record status is `DUE` and due date is within 3 days |
| **Category** | `fees` |
| **Recipients** | Parent who owns the fee record |
| **Title** | `"Fee due: $feeTitle"` |
| **Body** | `"$currency $amount • due $dueDate"` |
| **Deep link** | `parent/fees` |
| **Ref type** | `fee_record` |
| **Push priority** | High (financial) |
| **Sound** | Alert tone |
| **Channel** | `fees` |
| **Implementation** | Server-side scheduled job (cron or Quartz). Query `fee_records WHERE status='DUE' AND due_date BETWEEN now() AND now()+3days`. Call `Notify.toUser()` for each. |
| **Corner cases** | Fee already paid between query and send → stale notification (acceptable). Fee in DUE state with no due date → skip (no date to check). Parent has multiple due fees → multiple notifications (consider grouping). Currency formatting must match existing `NotificationsRouting.kt` synth bridge logic. |

#### Fee Overdue Reminder

| Field | Value |
|---|---|
| **Trigger** | Scheduled job — fee record status is `OVERDUE` |
| **Category** | `fees` |
| **Recipients** | Parent who owns the fee record |
| **Title** | `"Overdue: $feeTitle"` |
| **Body** | `"$currency $amount • was due $dueDate"` |
| **Deep link** | `parent/fees` |
| **Ref type** | `fee_record` |
| **Push priority** | High |
| **Sound** | Urgent alert tone |
| **Channel** | `fees` |
| **Implementation** | Same scheduled job as fee due. Query `fee_records WHERE status='OVERDUE'`. Throttle to once per week per fee record (track `last_reminded_at` column — needs migration). |
| **Corner cases** | Fee paid but status not updated → stale overdue notification. Need a `last_reminded_at` column to avoid spamming. Overdue fee with no parent → skip. |

#### Calendar Event Reminder (Upcoming)

| Field | Value |
|---|---|
| **Trigger** | Scheduled job — calendar event is within 24 hours |
| **Category** | `calendar` |
| **Recipients** | Users who were notified of the event originally |
| **Title** | `"Upcoming: $eventTitle"` |
| **Body** | `"$eventTitle is tomorrow at $time."` |
| **Deep link** | `calendar/{eventId}` |
| **Ref type** | `calendar_event` |
| **Push priority** | Default |
| **Sound** | Default tone |
| **Channel** | `calendar_events` |
| **Implementation** | Scheduled job. Query `calendar_events WHERE start_time BETWEEN now() AND now()+24h AND reminder_sent = false`. Need a `reminder_sent` boolean column (migration). |
| **Corner cases** | Event cancelled after reminder scheduled → stale reminder. Event with no start time → skip. All-day event → reminder at 8 AM morning of. |

#### Teacher De-Provisioned

| Field | Value |
|---|---|
| **Trigger** | Admin deactivates a teacher account |
| **Category** | `general` |
| **Recipients** | The deactivated teacher |
| **Title** | `"Account deactivated"` |
| **Body** | `"Your account has been deactivated. Contact your school administrator."` |
| **Deep link** | None (logged out) |
| **Push priority** | High |
| **Sound** | Default tone |
| **Channel** | `general_notifications` |
| **Implementation** | Call `Notify.toUser()` before deactivation. Also call `DeviceTokenRepository.deactivateAllForUser()` to stop future pushes. |
| **Corner cases** | Teacher already deactivated → skip. Teacher has no device tokens → in-app only (but they can't log in anyway). |

#### Parent Unlinked from Child

| Field | Value |
|---|---|
| **Trigger** | Admin or system unlinks a parent from a child |
| **Category** | `link_request` |
| **Recipients** | The parent |
| **Title** | `"Child link removed"` |
| **Body** | `"$childName has been unlinked from your account. Contact your school for details."` |
| **Deep link** | `parent/dashboard` |
| **Push priority** | High |
| **Sound** | Default tone |
| **Channel** | `link_requests` |
| **Corner cases** | Parent has multiple children → only notify about the unlinked one. |

#### Exam Schedule Published

| Field | Value |
|---|---|
| **Trigger** | Admin publishes exam timetable |
| **Category** | `academic` |
| **Recipients** | Parents of affected classes |
| **Title** | `"Exam schedule published"` |
| **Body** | `"Exam timetable for $examName is now available."` |
| **Deep link** | `parent/academics` |
| **Ref type** | `exam_schedule` |
| **Push priority** | Default |
| **Sound** | Default tone |
| **Channel** | `academic_updates` |
| **Corner cases** | Multiple classes → batch fan-out. Exam schedule edited → no re-notify. |

#### School Closure / Holiday Notice

| Field | Value |
|---|---|
| **Trigger** | Admin posts a school-wide closure notice |
| **Category** | `announcement` |
| **Recipients** | All parents + teachers in school |
| **Title** | `"School closure notice"` |
| **Body** | Closure details |
| **Deep link** | `announcements/{id}` |
| **Push priority** | High |
| **Sound** | Alert tone |
| **Channel** | `announcements` |
| **Corner cases** | Currently handled via announcement flow. Could be a special category for distinct sound/icon. |

#### OTP / Authentication Notification

| Field | Value |
|---|---|
| **Trigger** | OTP sent for authentication |
| **Category** | `auth` |
| **Recipients** | The user authenticating |
| **Title** | `"Your verification code"` |
| **Body** | OTP code (or masked) |
| **Deep link** | None |
| **Push priority** | High |
| **Sound** | Distinct OTP tone |
| **Channel** | `auth` |
| **Implementation** | Currently OTP is sent via SMS gateway (OTPSender). Could also send via FCM data message for instant delivery. |
| **Corner cases** | OTP expires → notification should be cancelled. User on SMS-only flow → skip FCM. Rate limiting → don't send more than 3 OTP notifications per hour. |

### 5.3 Notification Persistence & Dismissibility

All notifications follow the same persistence model:

| Property | Value | Notes |
|---|---|---|
| **Stored in DB** | ✅ Yes — every notification is a row in `notifications` table | `Notify.toUsers()` always writes to DB before attempting push |
| **Persistent in inbox** | ✅ Yes — notifications remain in the inbox until the user reads them | No auto-expiry; no TTL on in-app rows |
| **System tray dismissible** | ✅ Yes — `setAutoCancel(true)` in `NotificationManagerHelper` | Tapping or swiping dismisses the system tray notification |
| **Inbox dismissible** | ❌ No — currently no delete/swipe-to-dismiss in `NotificationsScreenV2` | See §8.6 for proposed delete action |
| **Read state persisted** | ✅ Yes — `isRead` + `readAt` columns in `notifications` table | `PATCH /api/v1/notifications/{id}/read` |
| **Mark all read** | ✅ Yes — `POST /api/v1/notifications/read-all` | Bulk updates all unread → read for `jwt.sub` |
| **Ephemeral (not stored)** | ❌ None — all notification types are persisted | OTP via FCM (if implemented) should be ephemeral — see below |
| **Soft delete / archive** | ❌ Not implemented | See §7.7 for proposed design |
| **Auto-cleanup** | ❌ Not implemented | Consider a job to delete notifications older than 90 days (see §7.7) |

**OTP exception**: If OTP is sent via FCM (currently SMS-only), it should be ephemeral — display in system tray but do NOT write to `notifications` table. OTP notifications should auto-cancel after 5 minutes (the OTP expiry window).

**Synth bridge items**: The parent-only synth items (announcements + fees in `NotificationsRouting.kt`) are NOT persisted — they have synthetic IDs (`ann_*`, `fee_*`) and are always `unread=true`. They cannot be marked read. Once real notifications replace them (after the Notify bridge is implemented), these synth items should be removed.

---

## 6. Backend Changes Required

### 6.1 CRITICAL: Bridge `Notify.toUsers()` → `NotificationService.send()` (FCM Push)

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notifications/Notify.kt`

**Current state**: `Notify.toUsers()` only writes DB rows to `NotificationsTable`. It does NOT call `NotificationService.send()` to push via FCM. All 17 trigger sites create in-app notifications but never send a push.

**Exact change**: Add a `NotificationService` instance and call `send()` after the `batchInsert`:

```kotlin
// Add imports
import com.littlebridge.enrollplus.feature.notification.dto.SendNotificationRequest
import com.littlebridge.enrollplus.feature.notification.repository.DeviceTokenRepository
import com.littlebridge.enrollplus.feature.notification.service.NotificationService

// Add module-level singletons (same pattern as NotificationRouting.kt)
private val deviceTokenRepository = DeviceTokenRepository()
private val pushService = NotificationService(deviceTokenRepository)

// In toUsers(), after the existing dbQuery { batchInsert ... } block:
runCatching {
    pushService.send(SendNotificationRequest(
        title = title,
        body = body,
        userIds = recipients.map { it.toString() },
        deepLink = deepLink,
        data = buildMap {
            put("type", category)  // KEY_TYPE on client reads this
            refType?.let { put("refType", it) }
            refId?.let { put("refId", it) }
            schoolId?.let { put("schoolId", it.toString()) }
            actorId?.let { put("actorId", it.toString()) }
        }
    ))
}
```

**Key contract**: The `data["type"]` key carries the category string. The client's `VidyaPrayagFirebaseMessagingService.KEY_TYPE` reads this and passes it to `NotificationManagerHelper.displayNotification(type=...)` which selects the correct notification channel.

**Corner cases**:
- `recipients` is empty → `send()` returns `sent=0` (already handled)
- Firebase not configured → `send()` returns `success=false` (already handled gracefully)
- Some recipients have no device tokens → they get the in-app row but no push (correct)
- `send()` throws unexpectedly → `runCatching` swallows it, in-app notification still delivered
- Large recipient list (1000+) → `send()` chunks at 500 tokens per FCM multicast (already handled)
- The `toUser()` convenience method delegates to `toUsers()`, so it's automatically covered

### 6.2 CRITICAL: Fix Logout — Clear FCM Token Cache + Deactivate on Server

#### 6.2.1 Client-side: Clear FCM token cache on logout

**File**: `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/auth/data/repository/AuthRepositoryImpl.kt:169`

**Current state**: `logout()` calls `preferenceRepository.clearSession()`. The Android DataStore implementation (`PreferenceManager.kt:147`) **already clears `FCM_TOKEN_KEY`** in `clearSession()`. However, the JS/wasm implementations (`LocalStoragePreferenceManager`, `InMemoryPreferenceManager`) do **NOT** clear the FCM token.

**Fix for Android**: ✅ Already handled — `PreferenceManager.clearSession()` removes `FCM_TOKEN_KEY` and `NOTIFICATIONS_DECLINED_KEY`.

**Fix for JS/wasm** — add FCM token clearing to `LocalStoragePreferenceManager.clearSession()`:
```kotlin
override suspend fun clearSession() {
    userRole.value = "GUEST"
    userToken.value = null
    userId.value = null
    refreshToken.value = null
    profileCompleted.value = null
    userName.value = null
    // Notification foundation: drop the cached FCM token (matches Android DataStore).
    fcmToken.value = null
    write(KEY_ROLE, "GUEST")
    write(KEY_TOKEN, null)
    write(KEY_USER_ID, null)
    write(KEY_REFRESH, null)
    write(KEY_PROFILE, null)
    write(KEY_USER_NAME, null)
    write(KEY_FCM_TOKEN, null)
}
```

**Also add the `fcmToken` StateFlow + getter/setter to `LocalStoragePreferenceManager`** (currently missing):
```kotlin
private val fcmToken = MutableStateFlow(read(KEY_FCM_TOKEN))

override fun getFcmToken(): Flow<String?> = fcmToken
override suspend fun setFcmToken(token: String?) {
    fcmToken.value = token
    write(KEY_FCM_TOKEN, token)
}

// In companion object:
const val KEY_FCM_TOKEN = "vp.fcmToken"
```

**Belt-and-suspenders approach** (recommended): Also explicitly clear in `AuthRepositoryImpl.logout()` after `clearSession()`:
```kotlin
preferenceRepository.clearSession()
// Explicit belt-and-suspenders — ensures FCM token is cleared even if
// a custom PreferenceRepository implementation forgets to clear it.
preferenceRepository.setFcmToken(null)
```

#### 6.2.2 Server-side: Deactivate device token on logout

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/auth/AuthRouting.kt:819`

**Current state**: `POST /logout` only revokes the refresh token session. It does NOT deactivate device tokens.

**Exact change**:
1. Add `fcmToken` field to `LogoutDto`:
```kotlin
@Serializable
data class LogoutDto(
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null
)
```

2. In the logout handler, after revoking the session, deactivate the token:
```kotlin
body?.fcmToken?.takeIf { it.isNotBlank() }?.let { token ->
    runCatching { deviceTokenRepository.deactivateToken(token) }
}
```

3. Client-side: Update `AuthApi.logout()` to send the FCM token in the request body.

**Corner cases**:
- `fcmToken` is null in the logout request → skip deactivation (backward compatible)
- Token already deactivated → `deactivateToken` updates 0 rows (no-op)
- User has multiple devices → only the logging-out device's token is deactivated (correct)

### 6.3 Remove Duplicate Device-Token Registration Endpoint

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notifications/NotificationsRouting.kt:252-282`

**Current state**: There are two endpoints that register device tokens:
- `POST /api/device-tokens` in `NotificationRouting.kt` — the canonical one, used by the client, with `schoolId` resolution and `appVersion`/`deviceModel` support
- `POST /api/v1/notifications/device-token` in `NotificationsRouting.kt` — a redundant, simpler version that nobody calls

**Exact change**: Remove lines 252-282 from `NotificationsRouting.kt` (the `post("/device-token")` block). Also remove the `DeviceTokenDto` class (lines 76-79) if it's not used elsewhere.

### 6.4 Add `category` to FCM Data Payload (No Code Change Needed)

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notification/service/NotificationService.kt`

The `buildDataPayload()` method already merges `request.data` into the payload. The `Notify` bridge (§6.1) passes `category` as `data["type"]`, so the client's `VidyaPrayagFirebaseMessagingService` reads it from `data[KEY_TYPE]` and routes to the correct channel.

**No change needed to `NotificationService` itself.**

### 6.5 Scheduled Notification Jobs

**New file**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notifications/NotificationScheduler.kt`

**Approach**: Use a simple coroutine-based scheduler launched at server startup.

```kotlin
package com.littlebridge.enrollplus.feature.notifications

import com.littlebridge.enrollplus.db.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.temporal.ChronoUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkFeeReminders() }
                    .onFailure { println("[$TAG] checkFeeReminders failed: ${it.message}") }
                runCatching { checkCalendarReminders() }
                    .onFailure { println("[$TAG] checkCalendarReminders failed: ${it.message}") }
            }
        }
    }

    /**
     * Fee reminders: find fee_records that are DUE within 3 days (or OVERDUE)
     * and haven't been reminded in the last 24 hours.
     */
    suspend fun checkFeeReminders() {
        val now = Instant.now()
        val threeDaysFromNow = now.plus(3, ChronoUnit.DAYS)
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)

        val dueFees = dbQuery {
            // Assuming FeeRecordsTable has: id, studentId, amount, status, dueDate, lastRemindedAt
            FeeRecordsTable.selectAll().where {
                (FeeRecordsTable.status inList listOf("DUE", "OVERDUE")) and
                (FeeRecordsTable.dueDate lessEq threeDaysFromNow) and
                (FeeRecordsTable.lastRemindedAt.isNull() or (FeeRecordsTable.lastRemindedAt less oneDayAgo))
            }.toList()
        }

        if (dueFees.isEmpty()) return

        for (row in dueFees) {
            val studentId = row[FeeRecordsTable.studentId]
            val feeId = row[FeeRecordsTable.id]
            val status = row[FeeRecordsTable.status]
            val dueDate = row[FeeRecordsTable.dueDate]

            // Resolve parents of this student
            val parentUserIds = resolveParentsOfStudent(studentId)
            if (parentUserIds.isEmpty()) continue

            val title = if (status == "OVERDUE") "Fee Overdue" else "Fee Due Soon"
            val body = "Fee payment of ${row[FeeRecordsTable.amount]} is due on $dueDate."
            val deepLink = "parent/fees"

            // Send in-app + push notification
            Notify.toUsers(
                userIds = parentUserIds,
                category = "fees",
                title = title,
                body = body,
                deepLink = deepLink,
                refType = "fee_record",
                refId = feeId.toString(),
            )

            // Update lastRemindedAt
            dbQuery {
                FeeRecordsTable.update(
                    { FeeRecordsTable.id eq feeId }
                ) {
                    it[FeeRecordsTable.lastRemindedAt] = now
                }
            }

            println("[$TAG] Fee reminder sent for fee $feeId to ${parentUserIds.size} parents")
        }
    }

    /**
     * Calendar reminders: find events starting within the next 24 hours
     * that haven't had a reminder sent yet.
     */
    suspend fun checkCalendarReminders() {
        val now = Instant.now()
        val twentyFourHoursFromNow = now.plus(24, ChronoUnit.HOURS)

        val upcomingEvents = dbQuery {
            // Assuming CalendarEventsTable has: id, schoolId, title, startTime, reminderSent
            CalendarEventsTable.selectAll().where {
                (CalendarEventsTable.startTime greater now) and
                (CalendarEventsTable.startTime lessEq twentyFourHoursFromNow) and
                (CalendarEventsTable.reminderSent eq false)
            }.toList()
        }

        if (upcomingEvents.isEmpty()) return

        for (row in upcomingEvents) {
            val eventId = row[CalendarEventsTable.id]
            val schoolId = row[CalendarEventsTable.schoolId]
            val eventTitle = row[CalendarEventsTable.title]
            val startTime = row[CalendarEventsTable.startTime]

            // Resolve all parents + teachers in this school
            val recipientIds = resolveSchoolMembers(schoolId)
            if (recipientIds.isEmpty()) continue

            val title = "Upcoming Event: $eventTitle"
            val body = "Event starts at $startTime"
            val deepLink = "calendar/$eventId"

            Notify.toUsers(
                userIds = recipientIds,
                category = "calendar",
                title = title,
                body = body,
                schoolId = schoolId,
                deepLink = deepLink,
                refType = "calendar_event",
                refId = eventId.toString(),
            )

            // Mark reminder as sent
            dbQuery {
                CalendarEventsTable.update(
                    { CalendarEventsTable.id eq eventId }
                ) {
                    it[CalendarEventsTable.reminderSent] = true
                }
            }

            println("[$TAG] Calendar reminder sent for event $eventId to ${recipientIds.size} recipients")
        }
    }

    /** Resolve parent user IDs for a student. */
    private suspend fun resolveParentsOfStudent(studentId: java.util.UUID): List<java.util.UUID> {
        return dbQuery {
            // Assuming StudentParentLinksTable links students to parent app_users
            StudentParentLinksTable.selectAll()
                .where { StudentParentLinksTable.studentId eq studentId }
                .map { it[StudentParentLinksTable.parentUserId] }
        }
    }

    /** Resolve all parent + teacher user IDs for a school. */
    private suspend fun resolveSchoolMembers(schoolId: java.util.UUID): List<java.util.UUID> {
        return dbQuery {
            AppUsersTable.selectAll()
                .where {
                    (AppUsersTable.schoolId eq schoolId) and
                    (AppUsersTable.role inList listOf("PARENT", "TEACHER"))
                }
                .map { it[AppUsersTable.id] }
        }
    }
}
```

**Exposed table additions needed** (see §7.1, §7.2):
```kotlin
// In Tables.kt — FeeRecordsTable:
val lastRemindedAt = timestamp("last_reminded_at").nullable()

// In Tables.kt — CalendarEventsTable:
val reminderSent = bool("reminder_sent").default(false)
```

**Server startup wiring** — in your Ktor `Application.module()` or `main`:
```kotlin
// Start the scheduler in a background coroutine scope
NotificationScheduler.start(CoroutineScope(Dispatchers.IO + SupervisorJob()))
```

**Corner cases**:
- Server restart → scheduler resumes (idempotent — queries are time-based)
- Multiple server instances → need distributed lock or leader election (out of scope for now — single-instance deploy)
- Job takes longer than interval → next tick overlaps (acceptable for notifications)
- DB query fails → `runCatching` swallows, retries next tick

### 6.6 Admin Broadcast School-Scoping (Security Fix)

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notification/api/NotificationRouting.kt:102`

**Current state**: `POST /api/admin/notifications/send` validates that the caller is a school admin but does NOT validate that the `userIds` in the request body belong to the admin's school. An admin could push to any user across any school.

**Exact change**: After `requireSchoolAdmin()` resolves the admin's `schoolId`, validate each `req.userIds` belongs to that school:
```kotlin
val schoolId = ctx.schoolId
val validUserIds = req.userIds.filter { uid ->
    runCatching {
        val userSchoolId = resolveSchoolIdForUser(UUID.fromString(uid))
        userSchoolId == schoolId
    }.getOrDefault(false)
}
val scopedReq = req.copy(userIds = validUserIds)
val response = notificationService.send(scopedReq)
```

### 6.7 Add `fcmToken` to Client `LogoutRequest`

**File**: `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/auth/domain/model/AuthModels.kt:103`

**Current state**: `LogoutRequest` only has `refreshToken`.

**Exact change**:
```kotlin
@Serializable
data class LogoutRequest(
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null
)
```

**File**: `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/auth/data/remote/AuthApi.kt:110`

**Current signature**:
```kotlin
suspend fun logout(token: String, refreshToken: String?): NetworkResult<ApiResponse<Unit>> {
    return safeApiCall {
        client.post(getUrl("api/v1/auth/logout")) {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequest(refreshToken))
        }
    }
}
```

**Exact change** — add `fcmToken` parameter and include it in the request body:
```kotlin
suspend fun logout(token: String, refreshToken: String?, fcmToken: String? = null): NetworkResult<ApiResponse<Unit>> {
    return safeApiCall {
        client.post(getUrl("api/v1/auth/logout")) {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequest(refreshToken = refreshToken, fcmToken = fcmToken))
        }
    }
}
```

**File**: `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/auth/data/repository/AuthRepositoryImpl.kt:169`

In `logout()`, pass the cached FCM token:
```kotlin
override suspend fun logout() {
    val token = preferenceRepository.getUserToken().first()
    val refreshToken = preferenceRepository.getRefreshToken().first()
    val fcmToken = preferenceRepository.getFcmToken().first()
    if (token != null) {
        runCatching { api.logout(token, refreshToken, fcmToken) }
    }
    preferenceRepository.clearSession()
    sessionManager.clearAuthCache()
    selectedChildHolder.clear()
}
```

### 6.8 FCM Service Account Configuration

**File**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notification/firebase/FirebaseAdminInitializer.kt`

The Firebase Admin SDK is initialized via `FirebaseAdminInitializer.app()`. Credential resolution (first non-blank wins):

| Priority | Source | Env Var / Key | Use Case |
|---|---|---|---|
| 1 | Inline JSON env var | `FIREBASE_CREDENTIALS_JSON` | Render / Railway / Fly.io (no file mount) |
| 2 | Credentials file path | `FIREBASE_CREDENTIALS_FILE` | Docker secret mount |
| 3 | Google ADC | `GOOGLE_APPLICATION_CREDENTIALS` | Standard Google convention |
| 4 | local.properties | `firebase.credentials.file` | Local dev |
| 5 | Application Default Credentials | (auto) | GCE / Cloud Run |

**Graceful degradation**: When no credentials resolve, `app()` returns `null`, `NotificationService.send()` returns `success=false, sent=0`. The server boots and all non-push features work normally.

**Multi-project support**: The OTPSender gateway uses a separate Firebase project:
- `OTP_SENDER_FIREBASE_CREDENTIALS_JSON` — inline JSON
- `OTP_SENDER_FIREBASE_CREDENTIALS_FILE` — file path
- `otp.sender.firebase.credentials.file` — local.properties key
- No ADC fallback (would resolve to the wrong project)

**No change needed** — this is already implemented. Documented here for completeness.

### 6.9 FCM Message TTL (Time-to-Live)

**Current state**: `NotificationService.send()` does NOT set a TTL on FCM messages. FCM defaults to 4 weeks (2,419,200 seconds) for messages with a `notification` block.

**Recommendation**: Set TTL per category based on urgency:

| Category | TTL | Rationale |
|---|---|---|
| `attendance` | 1 hour (3600s) | Time-sensitive — stale attendance alert is useless |
| `leave` | 1 hour | Action required promptly |
| `link_request` | 24 hours (86400s) | Admin action needed but not urgent |
| `marks` | 24 hours | Parents check within a day |
| `homework` | 24 hours | Due date context |
| `announcement` | 7 days (604800s) | Announcements remain relevant |
| `fees` | 7 days | Financial reminders stay valid |
| `message` | 24 hours | Read within a day |
| `calendar` | 1 hour | Event is imminent |
| `auth` (OTP) | 5 minutes (300s) | OTP expires in 5 min |
| `general` | 24 hours | Default |

**Exact change** in `NotificationService.kt`, in the `MulticastMessage.builder()` chain:
```kotlin
val ttlSeconds = when (request.data["type"]) {
    "attendance", "leave", "calendar" -> 3600L
    "auth" -> 300L
    "announcement", "fees" -> 604800L
    else -> 86400L
}
val multicast = MulticastMessage.builder()
    .addAllTokens(chunk)
    .setNotification(notification)
    .putAllData(dataPayload)
    .setTtl(ttlSeconds * 1000) // FCM expects milliseconds
    .build()
```

### 6.10 FCM Collapse Keys

**Current state**: No collapse key is set. If a teacher marks attendance twice (edit), the parent gets two separate system tray notifications.

**Recommendation**: Set `collapseKey` per category + entity to prevent notification flooding:

| Collapse Key | Format | Example |
|---|---|---|
| Attendance | `attendance_{studentId}_{date}` | `attendance_STU-001_2026-06-27` |
| Marks | `marks_{assessmentId}` | `marks_ASSESS-42` |
| Homework | `homework_{homeworkId}` | `homework_HW-15` |
| Announcement | `announcement_{announcementId}` | `announcement_ANN-8` |
| Leave | `leave_{leaveRequestId}` | `leave_LR-30` |
| Link request | `link_{linkRequestId}` | `link_LREQ-12` |
| Fees | `fees_{feeRecordId}` | `fees_FR-99` |
| Message | `message_{threadId}` | `message_TH-7` |
| Calendar | `calendar_{eventId}` | `calendar_EVT-3` |

**Exact change** in `NotificationService.kt`:
```kotlin
// In buildDataPayload or send():
val collapseKey = request.data["type"]?.let { type ->
    request.data["entityId"]?.let { id -> "${type}_${id}" }
}
// Then in MulticastMessage.builder():
collapseKey?.let { builder.setCollapseKey(it) }
```

**Note**: The `Notify` bridge (§6.1) must pass `entityId` (from `refId`) in the `data` map for collapse keys to work.

### 6.11 Retry Behaviour

**Current state**: No retry. If `sendEachForMulticast` fails for a chunk (network, quota, auth), all tokens in that chunk are counted as failed but left active. The next notification trigger will retry naturally.

**Recommendation**: No active retry needed for MVP. The design is intentionally fire-and-forget:
- In-app notification is always delivered (DB write succeeds independently)
- Push is best-effort (wrapped in `runCatching`)
- Invalid tokens are deactivated (won't be retried)
- Transient failures (quota, network) leave tokens active → next trigger retries naturally
- Scheduled jobs (fee/calendar reminders) retry on their next tick (hourly)

**Future enhancement**: If retry is needed, add an exponential backoff queue:
1. Failed sends → write to `notification_retry_queue` table with `next_attempt_at`
2. Background job picks up due retries
3. Max 3 attempts, then give up

### 6.12 Logging Strategy

**Current state**: `println()` statements scattered across `NotificationService`, `FirebaseAdminInitializer`, and `DeviceTokenRegistrar`.

**Recommended log events**:

| Event | Level | Message Format | Where |
|---|---|---|---|
| Firebase init success | INFO | `FIREBASE_INIT: FirebaseApp '{name}' initialized using '{source}'` | `FirebaseAdminInitializer` |
| Firebase init failure | WARN | `FIREBASE_INIT: No credentials resolved — push dispatch DISABLED` | `FirebaseAdminInitializer` |
| Push dispatch start | DEBUG | `NOTIFY_DISPATCH: sending to {n} tokens for {m} users` | `NotificationService.send()` |
| Push batch success | DEBUG | `NOTIFY_DISPATCH: batch sent={sent} failed={failed}` | `NotificationService.send()` |
| Push batch failure | WARN | `NOTIFY_DISPATCH: multicast batch failed for {n} tokens: {error}` | `NotificationService.send()` |
| Token deactivated | INFO | `NOTIFY_DISPATCH: deactivated invalid token …{last6}` | `handleFailure()` |
| Token deactivation failed | WARN | `NOTIFY_DISPATCH: failed to deactivate token: {error}` | `handleFailure()` |
| Token registration | INFO | `DEVICE_TOKEN: registered token …{last6} for user {uid}` | `DeviceTokenRepository.upsertToken()` |
| Token registration failed | WARN | `DEVICE_TOKEN: registration failed for user {uid}: {error}` | `DeviceTokenRegistrar` |
| Notify.toUsers called | DEBUG | `NOTIFY: toUsers recipients={n} category={cat} school={sid}` | `Notify.toUsers()` |
| Notify bridge push failed | WARN | `NOTIFY: push bridge failed for {n} recipients: {error}` | `Notify.toUsers()` (runCatching) |

**No PII in logs**: Never log full FCM tokens, full notification bodies, or user phone numbers. Always mask tokens (`…${token.takeLast(6)}`).

### 6.13 Rate Limiting

**Current state**: No rate limiting on notification dispatch. A teacher marking attendance for 100 students triggers 100+ `Notify.toUsers()` calls in one request.

**Recommendation**:

| Scope | Limit | Implementation |
|---|---|---|
| Per-user per-day | Max 50 notifications | `Notify.toUsers()` checks count of notifications created today for each recipient; skips users who have exceeded the limit |
| Per-category per-hour | Max 10 of same category | Prevents spamming (e.g., 10 attendance edits → only first notification) |
| OTP via FCM | Max 3 per hour | Already documented in §5 OTP corner cases |
| Admin broadcast | Max 5 per hour per school | Prevents admin from flooding all parents |

**Implementation**: Add a pre-check in `Notify.toUsers()`:
```kotlin
// Before batchInsert:
val todayCount = dbQuery {
    NotificationsTable.selectAll()
        .where { (NotificationsTable.userId inList recipients) and
                  (NotificationsTable.createdAt greaterEq todayStart()) }
        .count()
}
val rateLimited = recipients.filter { uid -> /* check per-user count */ }
val toNotify = recipients - rateLimited
```

**Corner cases**:
- Rate limit check fails (DB error) → proceed without limit (fail-open, not fail-closed)
- Scheduled jobs (fee reminders) → exempt from per-user limit (they're already throttled by `last_reminded_at`)

---

## 7. Database Changes Required

### 7.0 Existing Table Schemas (Reference)

#### `notifications` table

| Column | Type | Nullable | Default | Constraints | Index |
|---|---|---|---|---|---|
| `id` | uuid | No | `gen_random_uuid()` | Primary key | PK |
| `school_id` | uuid | Yes | — | — | — |
| `user_id` | uuid | No | — | Recipient `app_users.id` | — |
| `category` | varchar(32) | No | `'general'` | — | — |
| `title` | text | No | — | — | — |
| `body` | text | No | `''` | — | — |
| `deep_link` | text | Yes | — | — | — |
| `actor_id` | uuid | Yes | — | Who triggered it | — |
| `ref_type` | varchar(32) | Yes | — | e.g. `attendance_record` | — |
| `ref_id` | text | Yes | — | — | — |
| `is_read` | bool | No | `false` | — | — |
| `created_at` | timestamp | No | — | — | — |
| `read_at` | timestamp | Yes | — | Set when marked read | — |

**Missing indexes** (should be added):
- `ix_notifications_user_created` on `(user_id, created_at DESC)` — speeds up inbox list query
- `ix_notifications_user_unread` on `(user_id, is_read)` where `is_read = false` — speeds up unread count
- `ix_notifications_school` on `(school_id)` — admin queries by school

**Missing constraints**:
- FK `user_id` → `app_users(id) ON DELETE CASCADE` (currently no FK)
- FK `school_id` → `schools(id) ON DELETE SET NULL` (currently no FK)
- FK `actor_id` → `app_users(id) ON DELETE SET NULL` (currently no FK)

#### `device_tokens` table

| Column | Type | Nullable | Default | Constraints | Index |
|---|---|---|---|---|---|
| `id` | uuid | No | `gen_random_uuid()` | Primary key | PK |
| `school_id` | uuid | Yes | — | — | — |
| `user_id` | uuid | No | — | Owner `app_users.id` | `ix_device_tokens_user_active(user_id, is_active)` |
| `token` | text | No | — | Globally unique (FCM one-per-install) | `ux_device_tokens_token` (unique) |
| `platform` | varchar(16) | No | `'android'` | `android` / `ios` / `web` | — |
| `app_version` | varchar(64) | Yes | — | — | — |
| `device_model` | varchar(128) | Yes | — | — | — |
| `is_active` | bool | No | `true` | — | — |
| `created_at` | timestamp | No | — | — | — |
| `updated_at` | timestamp | No | — | — | — |
| `last_seen_at` | timestamp | Yes | — | — | — |

**Existing indexes**: `ux_device_tokens_token` (unique on `token`), `ix_device_tokens_user_active` (on `user_id, is_active`).

**Missing constraints**:
- FK `user_id` → `app_users(id) ON DELETE CASCADE` (currently no FK)
- FK `school_id` → `schools(id) ON DELETE SET NULL` (currently no FK)

### 7.1 `fee_records.last_reminded_at` (New Column)

**Purpose**: Track when a fee reminder was last sent, to avoid spamming parents with repeated notifications.

**Migration file**: `docs/db/migration-XXX-fee-reminder-tracking.sql`

```sql
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS last_reminded_at timestamp NULL;
```

**Exposed table update**: `server/.../db/Tables.kt` — `FeeRecordsTable`
```kotlin
val lastRemindedAt = timestamp("last_reminded_at").nullable()
```

**Usage**: The `NotificationScheduler.checkFeeReminders()` job queries:
```sql
WHERE status = 'DUE'
  AND due_date <= now() + interval '3 days'
  AND (last_reminded_at IS NULL OR last_reminded_at < now() - interval '24 hours')
```

After sending, update: `SET last_reminded_at = now()`.

### 7.2 `calendar_events.reminder_sent` (New Column)

**Purpose**: Track whether a 24-hour reminder has been sent for a calendar event.

**Migration file**: `docs/db/migration-XXX-calendar-reminder-tracking.sql`

```sql
ALTER TABLE calendar_events ADD COLUMN IF NOT EXISTS reminder_sent boolean NOT NULL DEFAULT false;
```

**Exposed table update**: `server/.../db/Tables.kt` — `CalendarEventsTable`
```kotlin
val reminderSent = bool("reminder_sent").default(false)
```

**Usage**: The `NotificationScheduler.checkCalendarReminders()` job queries:
```sql
WHERE start_time BETWEEN now() AND now() + interval '24 hours'
  AND reminder_sent = false
```

After sending, update: `SET reminder_sent = true`.

### 7.3 `notification_preferences` (New Table)

**Purpose**: Per-user, per-category notification preferences (enable/disable, sound selection).

**Migration file**: `docs/db/migration-XXX-notification-preferences.sql`

```sql
CREATE TABLE IF NOT EXISTS notification_preferences (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    category varchar(32) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    sound varchar(64) NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    UNIQUE(user_id, category)
);

CREATE INDEX IF NOT EXISTS ix_notification_prefs_user
    ON notification_preferences(user_id);
```

**Exposed table update**: `server/.../db/Tables.kt`
```kotlin
object NotificationPreferencesTable : UUIDTable("notification_preferences", "id") {
    val userId = uuid("user_id")
    val category = varchar("category", 32)
    val enabled = bool("enabled").default(true)
    val sound = varchar("sound", 64).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("ux_notification_prefs_user_category", userId, category)
    }
}
```

**Usage**: `Notify.toUsers()` should check preferences before inserting + pushing. If a user has `enabled=false` for a category, skip them.

### 7.4 `notifications.idempotency_key` (New Column — Optional)

**Purpose**: Prevent duplicate notifications when the same event triggers `Notify.toUsers()` multiple times (e.g. double-tap on submit, retry).

```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS idempotency_key varchar(128) NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_idempotency
    ON notifications(idempotency_key)
WHERE idempotency_key IS NOT NULL;
```

**Exposed table update**: `server/.../db/Tables.kt` — `NotificationsTable`
```kotlin
val idempotencyKey = varchar("idempotency_key", 128).nullable()
```

**Usage**: `Notify.toUsers()` accepts an optional `idempotencyKey`. Before inserting, check if a row with the same key already exists for any of the recipients. If so, skip.

### 7.5 Summary of DB Changes

| Change | Type | Table | Column | Migration File |
|---|---|---|---|---|
| Fee reminder tracking | New column | `fee_records` | `last_reminded_at timestamp NULL` | `migration-XXX-fee-reminder-tracking.sql` |
| Calendar reminder tracking | New column | `calendar_events` | `reminder_sent boolean DEFAULT false` | `migration-XXX-calendar-reminder-tracking.sql` |
| Notification preferences | New table | `notification_preferences` | (see §7.3) | `migration-XXX-notification-preferences.sql` |
| Idempotency (optional) | New column | `notifications` | `idempotency_key varchar(128) NULL` | `migration-XXX-notification-idempotency.sql` |

### 7.6 Missing Indexes on `notifications` Table

**Purpose**: The inbox list query (`SELECT ... WHERE user_id = ? ORDER BY created_at DESC LIMIT 200`) and the unread count query (`SELECT count(*) WHERE user_id = ? AND is_read = false`) are full scans without indexes.

**Migration file**: `docs/db/migration-XXX-notification-indexes.sql`

```sql
CREATE INDEX IF NOT EXISTS ix_notifications_user_created
    ON notifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_notifications_user_unread
    ON notifications(user_id, is_read)
    WHERE is_read = false;

CREATE INDEX IF NOT EXISTS ix_notifications_school
    ON notifications(school_id)
    WHERE school_id IS NOT NULL;
```

### 7.7 Soft Delete / Archive (Proposed)

**Current state**: No soft delete or archive. Notifications remain forever. `is_read` is the only state transition.

**Problem**: Over time, a user's inbox grows unbounded. The `GET /api/v1/notifications` endpoint returns the latest 200, but the table grows without cleanup.

**Proposed design** (not MVP — Phase 8 hardening):

```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS archived_at timestamp NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_user_archived
    ON notifications(user_id, archived_at)
    WHERE archived_at IS NULL;
```

**Behaviour**:
- `GET /api/v1/notifications` queries `WHERE archived_at IS NULL` (only active notifications)
- A scheduled job archives notifications older than 90 days: `SET archived_at = now() WHERE created_at < now() - interval '90 days' AND archived_at IS NULL`
- Archived notifications are excluded from inbox and unread count
- No hard delete (keep for audit trail)
- User can optionally "delete" a notification (swipe in inbox) → sets `archived_at = now()`

**Exposed table update**: `server/.../db/Tables.kt` — `NotificationsTable`
```kotlin
val archivedAt = timestamp("archived_at").nullable()
```

**Migration file**: `docs/db/migration-XXX-notification-archive.sql`

---

## 8. Android Client Implementation

### 8.1 Per-Category Notification Channels

**File**: `composeApp/src/androidMain/kotlin/com/littlebridge/enrollplus/notification/NotificationManagerHelper.kt`

**What to do**:
1. Define channel constants for each category.
2. Create all channels in `createAllChannels()` (called from `EnRollPlusApp.onCreate()`).
3. In `displayNotification()`, accept a `channelId` parameter and use it.

**Channel definitions**:

| Channel ID | Name | Importance | Vibration | Lights |
|---|---|---|---|---|
| `general_notifications` | General Notifications | HIGH | Yes | Yes |
| `attendance_alerts` | Attendance Alerts | HIGH | Yes | Yes |
| `academic_updates` | Academic Updates | DEFAULT | Yes | No |
| `announcements` | Announcements | DEFAULT | Yes | No |
| `leave_requests` | Leave Requests | HIGH | Yes | Yes |
| `link_requests` | Link Requests | HIGH | Yes | Yes |
| `fees` | Fee Reminders | HIGH | Yes | Yes |
| `messages` | Messages | DEFAULT | Yes | No |
| `calendar_events` | Calendar Events | DEFAULT | Yes | No |
| `auth` | Authentication | HIGH | Yes | No |

**Implementation**:
```kotlin
object NotificationManagerHelper {
    // ... existing constants ...
    
    const val CHANNEL_ID_ATTENDANCE = "attendance_alerts"
    const val CHANNEL_ID_ACADEMIC = "academic_updates"
    const val CHANNEL_ID_ANNOUNCEMENTS = "announcements"
    const val CHANNEL_ID_LEAVE = "leave_requests"
    const val CHANNEL_ID_LINK = "link_requests"
    const val CHANNEL_ID_FEES = "fees"
    const val CHANNEL_ID_MESSAGES = "messages"
    const val CHANNEL_ID_CALENDAR = "calendar_events"
    const val CHANNEL_ID_AUTH = "auth"
    
    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        createChannel(nm, CHANNEL_ID_GENERAL, "General Notifications", "General school notifications", NotificationManager.IMPORTANCE_HIGH)
        createChannel(nm, CHANNEL_ID_ATTENDANCE, "Attendance Alerts", "Absent and late attendance notifications", NotificationManager.IMPORTANCE_HIGH)
        createChannel(nm, CHANNEL_ID_ACADEMIC, "Academic Updates", "Marks, homework, and exam notifications", NotificationManager.IMPORTANCE_DEFAULT)
        createChannel(nm, CHANNEL_ID_ANNOUNCEMENTS, "Announcements", "School announcements", NotificationManager.IMPORTANCE_DEFAULT)
        createChannel(nm, CHANNEL_ID_LEAVE, "Leave Requests", "Leave request updates", NotificationManager.IMPORTANCE_HIGH)
        createChannel(nm, CHANNEL_ID_LINK, "Link Requests", "Child-link request updates", NotificationManager.IMPORTANCE_HIGH)
        createChannel(nm, CHANNEL_ID_FEES, "Fee Reminders", "Fee due and overdue notifications", NotificationManager.IMPORTANCE_HIGH)
        createChannel(nm, CHANNEL_ID_MESSAGES, "Messages", "Teacher and admin messages", NotificationManager.IMPORTANCE_DEFAULT)
        createChannel(nm, CHANNEL_ID_CALENDAR, "Calendar Events", "Academic calendar reminders", NotificationManager.IMPORTANCE_DEFAULT)
        createChannel(nm, CHANNEL_ID_AUTH, "Authentication", "OTP and login notifications", NotificationManager.IMPORTANCE_HIGH)
    }
    
    fun channelIdForCategory(category: String?): String = when (category?.lowercase()) {
        "attendance" -> CHANNEL_ID_ATTENDANCE
        "marks", "homework", "academic" -> CHANNEL_ID_ACADEMIC
        "announcement" -> CHANNEL_ID_ANNOUNCEMENTS
        "leave" -> CHANNEL_ID_LEAVE
        "link_request" -> CHANNEL_ID_LINK
        "fees" -> CHANNEL_ID_FEES
        "message" -> CHANNEL_ID_MESSAGES
        "calendar" -> CHANNEL_ID_CALENDAR
        "auth" -> CHANNEL_ID_AUTH
        else -> CHANNEL_ID_GENERAL
    }
    
    private fun createChannel(nm: NotificationManager, id: String, name: String, desc: String, importance: Int) {
        if (nm.getNotificationChannel(id) != null) return
        val channel = NotificationChannel(id, name, importance).apply {
            description = desc
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(channel)
    }
}
```

**Update `displayNotification()`** to accept and use `category`:
```kotlin
fun displayNotification(
    context: Context,
    title: String,
    body: String,
    deepLink: String? = null,
    type: String? = null,        // existing — maps to category
    entityId: String? = null,
    schoolId: String? = null,
    notificationId: Int = nextNotificationId()
): Int {
    val channelId = channelIdForCategory(type)
    // ... use channelId in NotificationCompat.Builder ...
}
```

**Update `EnRollPlusApp.kt`**:
```kotlin
// Replace:
NotificationManagerHelper.createDefaultChannel(this)
// With:
NotificationManagerHelper.createAllChannels(this)
```

**Update `VidyaPrayagFirebaseMessagingService.kt`**:
The `type` field from the data payload is already passed as `type` to `displayNotification()`. The `type` value comes from the `data` map in the FCM payload, which the `Notify` bridge sets to `category`. So this should work once the bridge is in place.

**Corner cases**:
- Channel created with IMPORTANCE_HIGH → cannot be downgraded later (Android limitation). Must get it right the first time.
- User disables a channel in Android Settings → `NotificationManagerCompat.areNotificationsEnabled()` returns true but the channel is blocked. Check `nm.getNotificationChannel(id).importance == NotificationManager.IMPORTANCE_NONE` before posting.
- App updated with new channels → `createAllChannels()` is idempotent (checks `getNotificationChannel != null`).
- Android < O (API 26) → channels don't exist, `createAllChannels()` is a no-op. `NotificationCompat.Builder` with a channel ID is safe — it's ignored on < O.

### 8.2 Update `VidyaPrayagFirebaseMessagingService` to Pass Category

**File**: `composeApp/src/androidMain/kotlin/com/littlebridge/enrollplus/notification/VidyaPrayagFirebaseMessagingService.kt`

The service already extracts `type` from the data payload and passes it to `displayNotification()`. The `type` field should carry the `category` value from the server.

**Current key mapping** (line 62-67):
```kotlin
const val KEY_TYPE = "type"           // → used as channelIdForCategory(type) input
```

**No change needed** — once the `Notify` bridge passes `category` in the `data` map with key `"type"` (or the server's `buildDataPayload` maps it), the client will route to the correct channel.

**However**: The `Notify` bridge should pass `category` as `data["type"]` (not `data["category"]`) to match the existing client key contract. Alternatively, add `KEY_CATEGORY = "category"` to the client and read it. Either way, the key contract must be consistent.

**Recommended**: Use `data["type"] = category` in the `Notify` bridge, since the client already reads `KEY_TYPE`.

### 8.3 Permission Request Flow

**Current implementation**: `AndroidNotificationService.requestPermission()` calls `ActivityCompat.requestPermissions()` for `POST_NOTIFICATIONS` (Android 13+). `PermissionViewModel` checks `hasPermission()` on launch.

**Flow**:
1. App launches → `PermissionViewModel.checkNotificationPermission()` checks `NotificationManagerCompat.areNotificationsEnabled()`
2. If not granted AND Android 13+ → `requestPermission()` triggers system dialog
3. User grants → notifications display normally
4. User denies → `shouldShowRationale()` returns true on next launch → custom rationale dialog
5. User denies twice → `shouldShowRationale()` returns false → "Not Now" persisted in prefs → rationale never shown again

**BUG 4 (from §4.3)**: The `onRequestPermissionsResult` callback is never wired in `MainActivity`. The immediate callback is dead. The `PermissionViewModel` relies on `hasPermission()` polling on next launch, which works but is delayed.

**Fix**: Use `ActivityResultContracts.RequestPermission()` via `rememberLauncherForActivityResult` in the Compose layer (recommended — more idiomatic for Compose).

**Exact code** — add to each home screen (`ParentHomeScreenV2`, `SchoolHomeScreenV2`, `TeacherHomeScreenV2`):
```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ParentHomeScreenV2(
    // ... existing params ...
    permissionVm: PermissionViewModel = koinViewModel(),
) {
    // ... existing state ...

    // Compose-native permission launcher — replaces the dead ActivityCompat callback.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted — notifications will now show.
            // No need to call permissionVm; next checkNotificationPermission() will see it.
        }
        // If not granted, the rationale logic in PermissionViewModel handles re-prompting.
    }

    LaunchedEffect(Unit) {
        permissionVm.checkNotificationPermission()
        while (true) {
            delay(60_000L)
            viewModel.refreshLiveClock()
        }
    }

    // ... existing content ...

    VConfirmDialog(
        visible = showRationale,
        title = "Stay Informed",
        message = "Enable notifications to receive important updates about school events, attendance, and fee reminders.",
        confirmLabel = "Enable",
        onConfirm = {
            permissionVm.requestNotificationPermission()
            // Fire the Compose-native launcher instead of the dead ActivityCompat path.
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        },
        onDismiss = permissionVm::declineNotifications,
        cancelLabel = "Not Now",
        icon = VIcons.Bell,
    )
}
```

**Note**: The `permissionLauncher.launch()` call fires the system dialog. The result callback updates the OS state. `PermissionViewModel.checkNotificationPermission()` on the next launch will see the granted/denied state via `NotificationManagerCompat.areNotificationsEnabled()`.

### 8.4 Permission Denied Flow

| Scenario | Behaviour |
|---|---|
| User denies once | `shouldShowRationale()` returns true → show custom rationale dialog on next launch explaining why notifications are useful |
| User denies twice (permanently) | `shouldShowRationale()` returns false → persist "Not Now" in prefs → rationale never shown again |
| User denies then changes mind | Must go to Android Settings → App → Notifications. App cannot re-prompt. Provide a "Open settings" button in notification preferences screen |
| Android < 13 | Permission not required → `hasPermission()` returns true. No dialog shown |
| User grants but disables channel in Settings | `areNotificationsEnabled()` returns true but channel is blocked. Notification is silently dropped. Check `nm.getNotificationChannel(id).importance == NotificationManager.IMPORTANCE_NONE` before posting |

### 8.5 Foreground / Background / Terminated App Behaviour

| App State | FCM Payload Type | Behaviour |
|---|---|---|
| **Foreground** | `notification` block | `onMessageReceived()` IS called. System tray does NOT auto-display. App must call `displayNotification()` manually |
| **Foreground** | `data` block only | `onMessageReceived()` IS called. App extracts title/body from data and calls `displayNotification()` |
| **Background** | `notification` block | System tray auto-displays. `onMessageReceived()` is NOT called. Tap opens app via `PendingIntent` |
| **Background** | `data` block only | `onMessageReceived()` IS called (Android delivers data messages regardless of app state). App calls `displayNotification()` |
| **Terminated** | `notification` block | Same as background — system tray auto-displays |
| **Terminated** | `data` block only | `onMessageReceived()` IS called. App processes and displays |
| **Killed (swiped away)** | Either | FCM delivers. System tray displays (notification block) or `onMessageReceived` fires (data block). App is NOT launched unless user taps |

**Current implementation**: `NotificationService.send()` builds hybrid payloads (both `notification` + `data` blocks). This means:
- Background/terminated: system tray auto-renders the `notification` block. Deep link works via `PendingIntent`. But `onMessageReceived` is NOT called, so no foreground processing.
- Foreground: `onMessageReceived` IS called. The service extracts data and calls `displayNotification()`.

**Key contract**: The `data` block always carries `title`, `body`, `type`, `deepLink` so the foreground path can render. The `notification` block carries `title`, `body` so the background path auto-renders.

### 8.6 Badge Count

**Current state**: No badge count implementation. The unread count is shown only as a number in the inbox hero and as a red dot on the bell icon in portal headers.

**Android badge count**: Android 8+ supports notification badges (dots) on app icons per-channel. This is controlled by `NotificationChannel.setShowBadge(true)`.

**Recommendation**:
```kotlin
// In createChannel():
channel.setShowBadge(true)  // Enable badge for all channels
```

**In-app badge (bell icon)**: The bell icon in parent/teacher/school portal headers shows a red dot when `unreadCount > 0`. This is fetched via `GET /api/v1/notifications/summary` and cached in `NotificationsViewModel.state.unreadCount`.

**Corner cases**:
- User clears all notifications from settings → badge clears automatically
- User disables badges in Settings → app cannot override
- Badge count = number of unread notifications in that channel (Android auto-tracks)

### 8.7 Inbox UI — States, Read/Unread, Delete

**File**: `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/screens/notifications/NotificationsScreenV2.kt`

| UI Element | Current State | Notes |
|---|---|---|
| **Loading state** | ✅ Handled via `VStateHost(loading = state.isLoading)` | Shows spinner |
| **Error state** | ✅ Handled via `VStateHost(error = state.error)` | Shows error message + retry button |
| **Empty state** | ✅ Handled via `VStateHost(isEmpty = visible.isEmpty())` | Shows "You're all caught up" with check icon |
| **Empty (unread filter)** | ✅ Shows "No unread notifications." | Different message when filter is active |
| **Notification list** | ✅ Scrollable list with staggered fade-up entrance | `verticalScroll(rememberScrollState())` |
| **Read/unread visual** | ✅ Unread: teal-deep dot + raised shadow. Read: chevron icon + resting shadow | `if (n.unread) ... else ...` |
| **Filter pills** | ✅ "All" / "Unread · N" toggle | `filterUnread` state |
| **Mark as read** | ✅ Tap notification → `onMarkRead(n.id)` → `PATCH /api/v1/notifications/{id}/read` | Marks read, no navigation yet |
| **Mark all read** | ✅ "Mark all" button in header → `onMarkAll()` → `POST /api/v1/notifications/read-all` | Bulk update |
| **Pull-to-refresh** | ❌ Not implemented in notifications screen | See §8.7.1 below for exact wiring code |
| **Pagination** | ❌ Not implemented | Server returns latest 200 (hard limit). For MVP this is sufficient. Future: cursor-based pagination |
| **Delete action** | ❌ Not implemented | No swipe-to-dismiss. See §7.7 for proposed `archived_at` column |
| **Retry on error** | ✅ `onRetry = viewModel::load` passed to `VStateHost` | Re-fetches notifications |
| **Category badge** | ✅ `VBadge` with category-specific tone | `categoryBadgeTone(n.category)` |
| **Category icon** | ✅ Category-tinted icon tile | `categoryIcon(n.category)` + `categoryTile(n.category)` |
| **Time display** | ✅ Shows `n.time` (raw timestamp string from API) | Should be formatted as relative time ("2h ago") |

### 8.7.1 Pull-to-Refresh Wiring

**File**: `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/screens/notifications/NotificationsScreenV2.kt`

**Component**: `VPullRefresh` (`composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/components/VPullRefresh.kt`)

**Exact code** — wrap the notification list content in `VPullRefresh`:
```kotlin
import com.littlebridge.enrollplus.ui.v2.components.VPullRefresh

@Composable
fun NotificationsScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val filterUnread by viewModel.filterUnread.collectAsStateV2()
    val visible = remember(state.notifications, filterUnread) {
        if (filterUnread) state.notifications.filter { it.unread }
        else state.notifications
    }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    VScreenScaffold(
        modifier = modifier,
        topBar = {
            // ... existing header with back button, title, filter pills, mark-all ...
        },
    ) { padding ->
        VPullRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.load()
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Existing content: VStateHost + scrollable list
            if (state.isLoading && !isRefreshing) {
                // Show loading state
            } else if (state.error != null) {
                // Show error state with retry
            } else if (visible.isEmpty()) {
                // Show empty state
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    visible.forEachIndexed { index, notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { viewModel.markRead(notification.id) },
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }

    // Reset refresh state when loading completes
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) isRefreshing = false
    }
}
```

**Key points**:
- `VPullRefresh` wraps the entire content area (below the header)
- `isRefreshing` is set to `true` on pull, reset to `false` when `state.isLoading` becomes `false`
- The existing `VStateHost` loading state is suppressed during pull-to-refresh (the refresh spinner replaces it)
- `viewModel.load()` re-fetches from `GET /api/v1/notifications`

### 8.8 Local Notification Handling

**Current implementation**: `VidyaPrayagFirebaseMessagingService.onMessageReceived()` handles incoming FCM messages when the app is in foreground (or when data-only message arrives in any state):

1. Extract `title`, `body`, `type`, `deepLink`, `entityId`, `schoolId` from `remoteMessage.data`
2. If `title` is null, fall back to `remoteMessage.notification?.title`
3. Call `NotificationManagerHelper.displayNotification(context, title, body, deepLink, type, entityId, schoolId)`
4. `displayNotification()` creates a `NotificationCompat.Builder` with the correct channel ID, sets `setContentIntent(pendingIntent)`, and posts via `NotificationManagerCompat.notify()`

**Notification ID**: `nextNotificationId()` — incremented atomic counter. Each notification gets a unique ID for cancel/update.

### 8.8.1 Complete `displayNotification()` — Merged Implementation

The current `displayNotification()` only uses `CHANNEL_ID_GENERAL`. The complete version merges channel routing (§8.1), sound (§11), grouping (§15), and action buttons (§14) into one function:

```kotlin
/**
 * Complete displayNotification — merges channel routing, sound, grouping,
 * action buttons, and custom views into one call.
 *
 * Replace the existing NotificationManagerHelper.displayNotification() body.
 */
fun displayNotification(
    context: Context,
    title: String,
    body: String,
    deepLink: String? = null,
    type: String? = null,
    entityId: String? = null,
    schoolId: String? = null,
    notificationId: Int = nextNotificationId()
): Int {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        Log.w(TAG, "Notifications not enabled — suppressing (title=$title).")
        return notificationId
    }

    val channelId = channelIdForCategory(type)
    val pendingIntent = buildContentPendingIntent(context, deepLink, type, entityId, schoolId)

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(com.littlebridge.enrollplus.R.drawable.ic_app_mono)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setShowBadge(true)  // §8.6 — badge count on app icon

    // §11 — Per-category sound
    val soundUri = soundUriForCategory(type)
    if (soundUri != null) {
        builder.setSound(soundUri)
    }

    // §15 — Grouping/stacking
    val groupKey = collapseKeyForCategory(type, entityId)
    if (groupKey != null) {
        builder.setGroup(groupKey)
        // Post a summary notification for the group
        if (isFirstInGroup(context, groupKey)) {
            postGroupSummary(context, channelId, groupKey, title)
        }
    }

    // §14 — Action buttons (only for actionable categories)
    when (type) {
        "leave_request" -> {
            val approveIntent = buildActionIntent(context, "APPROVE", entityId, notificationId)
            val rejectIntent = buildActionIntent(context, "REJECT", entityId, notificationId)
            builder.addAction(0, "Approve", approveIntent)
            builder.addAction(0, "Reject", rejectIntent)
        }
        "link_request" -> {
            val approveIntent = buildActionIntent(context, "LINK_APPROVE", entityId, notificationId)
            builder.addAction(0, "Approve", approveIntent)
        }
    }

    // §6.10 — Collapse key (so newer notification supersedes older of same type)
    if (groupKey != null) {
        // Android grouping uses setGroup; FCM collapse key is server-side.
        // For local re-posts, use the same notificationId to update in place:
        // (caller can pass a derived id instead of nextNotificationId())
    }

    runCatching { NotificationManagerCompat.from(context).notify(notificationId, builder.build()) }
        .onFailure { e -> Log.e(TAG, "Failed to post notification id=$notificationId", e) }

    return notificationId
}

/** §8.1 — Map category to channel ID. */
fun channelIdForCategory(type: String?): String = when (type) {
    "attendance" -> "attendance_alerts"
    "marks", "exam_result" -> "academic_updates"
    "homework", "assignment" -> "homework_reminders"
    "announcement", "announcements" -> "announcements"
    "leave", "leave_request" -> "leave_notifications"
    "fees", "fee_reminder" -> "fee_reminders"
    "link_request", "child_link" -> "link_requests"
    "message", "messages" -> "messages"
    "calendar", "event_reminder" -> "calendar_reminders"
    "otp" -> "auth_notifications"
    else -> "general_notifications"
}

/** §11 — Map category to sound URI. Returns null for default sound. */
fun soundUriForCategory(type: String?): Uri? = when (type) {
    "attendance" -> Uri.parse("android.resource://${context.packageName}/raw/attendance_chime")
    "announcement", "announcements" -> Uri.parse("android.resource://${context.packageName}/raw/announcement_gong")
    "fees", "fee_reminder" -> Uri.parse("android.resource://${context.packageName}/raw/fee_alert")
    "otp" -> Uri.parse("android.resource://${context.packageName}/raw/otp_ping")
    else -> null  // Use channel default sound
}

/** §15 — Group key for stacking. */
fun collapseKeyForCategory(type: String?, entityId: String?): String? {
    if (type == null) return null
    return when (type) {
        "attendance" -> "group_attendance"
        "announcement", "announcements" -> "group_announcements"
        "fees", "fee_reminder" -> "group_fees"
        else -> null  // No grouping for other categories
    }
}

/** §15 — Post a group summary notification. */
fun postGroupSummary(context: Context, channelId: String, groupKey: String, title: String) {
    val summaryId = groupKey.hashCode()
    val summary = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(com.littlebridge.enrollplus.R.drawable.ic_app_mono)
        .setContentTitle(title)
        .setStyle(NotificationCompat.InboxStyle().setSummaryText("VidyaPrayag"))
        .setGroup(groupKey)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .build()
    runCatching { NotificationManagerCompat.from(context).notify(summaryId, summary) }
}

/** §14 — Build action PendingIntent for notification action buttons. */
fun buildActionIntent(
    context: Context,
    action: String,
    entityId: String?,
    notificationId: Int
): PendingIntent {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
        action = "com.littlebridge.enrollplus.NOTIFICATION_ACTION"
        putExtra("action_type", action)
        entityId?.let { putExtra("entity_id", it) }
        putExtra("notification_id", notificationId)
    }
    return PendingIntent.getBroadcast(
        context,
        (action + entityId).hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** §15 — Track if this is the first notification in a group (simple in-memory set). */
private val activeGroups = mutableSetOf<String>()
fun isFirstInGroup(context: Context, groupKey: String): Boolean {
    return activeGroups.add(groupKey)
}
```

### 8.9 Silent Notification Handling (Data-Only Push)

**Use case**: Background sync, badge count update, or in-app data refresh without showing a system tray notification.

**Current state**: Not implemented. All FCM messages include a `notification` block, so they always display in the system tray.

**Future implementation**: For silent pushes, omit the `notification` block in `NotificationService.send()` and set `contentAvailable = true`:

```kotlin
// In NotificationService, for silent push:
val message = Message.builder()
    .setToken(token)
    .putAllData(dataPayload)
    .setAndroidConfig(AndroidConfig.builder()
        .setPriority(AndroidConfig.Priority.NORMAL)
        .build())
    .build()
// No .setNotification() → data-only → silent
```

**Client handling**: `onMessageReceived()` fires. Check if `remoteMessage.notification == null` → silent push. Update badge count, refresh inbox, or trigger background sync without displaying a notification.

**Corner cases**:
- Doze mode may delay silent pushes
- App killed → `onMessageReceived` may not fire for data-only messages on some OEMs (aggressive battery management)

### 8.10 Tap Handling & Navigation

**Current state**: `NotificationManagerHelper.buildContentPendingIntent()` creates an `Intent` targeting `MainActivity` with deep link extras. But `MainActivity` never reads these extras (see §13.1).

**After fix (§13.2)**:
1. User taps notification → `PendingIntent` fires → `MainActivity.onCreate()` or `onNewIntent()`
2. `extractDeepLink(intent)` reads `EXTRA_DEEP_LINK` and `EXTRA_FROM_PUSH`
3. `deepLink` state passed to `App()` composable → `NavGraphV2`
4. `LaunchedEffect(deepLink)` parses path and navigates to correct screen
5. `onDeepLinkConsumed()` clears the state

**See §13 for full deep-link routing spec.**

---

## 9. iOS Client Implementation

### 9.1 `IOSNotificationService`

**New file**: `shared/src/iosMain/kotlin/com/littlebridge/enrollplus/notification/IOSNotificationService.kt`

```kotlin
class IOSNotificationService : NotificationService {
    override suspend fun syncDeviceToken(force: Boolean): Boolean {
        // Use FirebaseMessaging.instance.apnsToken (or token)
        // Register with backend via NotificationApi
    }
    
    override fun hasPermission(): Boolean {
        // UNUserNotificationCenter.current().getNotificationSettings()
    }
    
    override fun requestPermission(onResult: (Boolean) -> Unit) {
        // UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
    }
    
    override fun shouldShowRationale(): Boolean {
        // iOS doesn't have rationale — return false
    }
}
```

**Corner cases**:
- iOS provisional authorization → notifications appear silently in Notification Center
- iOS critical alerts → require Apple entitlement (separate capability)
- iOS notification groups → use `threadIdentifier` in UNNotificationContent
- APNs token vs FCM token → Firebase abstracts this; use `FirebaseMessaging.instance.token()`

### 9.2 iOS Notification Categories

iOS uses `UNNotificationCategory` with `UNNotificationAction` for action buttons.

```kotlin
// Define categories:
val markReadAction = UNNotificationAction(identifier: "MARK_READ", title: "Mark Read", options: [])
let approveAction = UNNotificationAction(identifier: "APPROVE", title: "Approve", options: [.foreground])
```

### 9.3 iOS Notification Sounds

```kotlin
UNNotificationContent().sound = UNNotificationSound(named: UNNotificationSoundName("notification_attendance.wav"))
```

Sound files go in `iosApp/iosApp/` bundle root (added to target).

---

## 10. Notification Channels

### 10.1 Channel-to-Category Mapping

| Server Category | Android Channel ID | Channel Name | Importance | Description |
|---|---|---|---|---|
| `attendance` | `attendance_alerts` | Attendance Alerts | HIGH | Absent/late notifications |
| `marks` | `academic_updates` | Academic Updates | DEFAULT | Marks, results, homework |
| `homework` | `academic_updates` | Academic Updates | DEFAULT | (shared with marks) |
| `announcement` | `announcements` | Announcements | DEFAULT | School announcements |
| `leave` | `leave_requests` | Leave Requests | HIGH | Leave request updates |
| `link_request` | `link_requests` | Link Requests | HIGH | Child-link updates |
| `fees` | `fees` | Fee Reminders | HIGH | Fee due/overdue |
| `message` | `messages` | Messages | DEFAULT | Teacher/admin messages |
| `calendar` | `calendar_events` | Calendar Events | DEFAULT | Calendar reminders |
| `auth` | `auth` | Authentication | HIGH | OTP/login |
| `general` (default) | `general_notifications` | General Notifications | HIGH | Fallback |

### 10.2 Channel Behavior

| Channel | Heads-up | Vibration | LED | Sound |
|---|---|---|---|---|
| `attendance_alerts` | Yes (HIGH) | Yes | Yes | Custom: `alert_attendance.mp3` |
| `academic_updates` | No (DEFAULT) | Yes | No | Default |
| `announcements` | No (DEFAULT) | Yes | No | Default |
| `leave_requests` | Yes (HIGH) | Yes | Yes | Default |
| `link_requests` | Yes (HIGH) | Yes | Yes | Custom: `success_chime.mp3` (approved) |
| `fees` | Yes (HIGH) | Yes | Yes | Custom: `alert_fee.mp3` |
| `messages` | No (DEFAULT) | Yes | No | Custom: `message_pop.mp3` |
| `calendar_events` | No (DEFAULT) | Yes | No | Default |
| `auth` | Yes (HIGH) | Yes | No | Custom: `otp_ping.mp3` |
| `general_notifications` | Yes (HIGH) | Yes | Yes | Default |

### 10.3 Channel Update / Migration

**Corner case**: Changing a channel's importance after creation.
- Android does NOT allow upgrading or downgrading importance after a channel is created.
- The user can manually change it in Settings, but the app cannot.
- To "reset" a channel, delete it (`nm.deleteNotificationChannel(id)`) and recreate — but this resets user preferences.
- **Strategy**: Get importance right the first time. Never change it.

---

## 11. Notification Sounds

### 11.1 Sound File Placement

**Android**: `composeApp/src/androidMain/res/raw/`
```
res/raw/
  alert_attendance.mp3    — urgent ping for absent/late
  alert_fee.mp3           — coin/ching for fee reminders
  success_chime.mp3       — positive chime for approved links
  message_pop.mp3         — soft pop for messages
  otp_ping.mp3            — short ping for OTP
  notification_default.mp3 — fallback for categories without custom sound
```

**iOS**: `iosApp/iosApp/` (bundle root, added to target)
```
alert_attendance.wav
alert_fee.wav
success_chime.wav
message_pop.wav
otp_ping.wav
notification_default.wav
```

> **Note**: iOS requires `.wav`, `.aiff`, or `.caf` format (not `.mp3`).

### 11.2 Wiring Sounds to Channels (Android)

**In `NotificationManagerHelper.createAllChannels()`**:

```kotlin
private fun createChannelWithSound(
    nm: NotificationManager,
    id: String,
    name: String,
    desc: String,
    importance: Int,
    soundResource: String? = null,
) {
    if (nm.getNotificationChannel(id) != null) return
    val channel = NotificationChannel(id, name, importance).apply {
        description = desc
        enableVibration(true)
        enableLights(true)
        if (soundResource != null) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/raw/$soundResource")
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(soundUri, audioAttributes)
        }
    }
    nm.createNotificationChannel(channel)
}
```

**Corner cases**:
- Sound file missing → channel falls back to default system sound
- User disables sound for channel in Settings → app cannot override
- Sound file too long (>5 sec) → Android truncates; keep sounds under 3 seconds
- Sound file too large (>1 MB) → may cause ANR; keep under 100 KB
- Do NOT use copyrighted sounds

### 11.3 Per-Notification Sound Override

For cases where the same channel needs different sounds (e.g., `link_requests` channel: success for approved, neutral for rejected):

```kotlin
// In displayNotification():
val soundUri = when (type) {
    "link_request" -> if (body.contains("approved", ignoreCase = true))
        Uri.parse("android.resource://${context.packageName}/raw/success_chime")
    else null // use channel default
    else -> null
}
if (soundUri != null) {
    builder.setSound(soundUri)
}
```

**Corner case**: `setSound()` on the notification builder overrides the channel sound for that specific notification only. The channel's default sound is unchanged.

---

## 12. Custom Notification Views

### 12.1 Custom Layouts (Android)

**Layout files**: `composeApp/src/androidMain/res/layout/`

#### Compact view (`notification_compact.xml`)
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:orientation="horizontal"
    android:padding="12dp"
    android:background="@color/notification_bg">
    
    <ImageView
        android:id="@+id/notif_icon"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:src="@drawable/ic_attendance" />
    
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:layout_marginStart="12dp"
        android:orientation="vertical">
        
        <TextView
            android:id="@+id/notif_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="@color/notification_title"
            android:textSize="14sp"
            android:textStyle="bold"
            android:maxLines="1" />
        
        <TextView
            android:id="@+id/notif_body"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="@color/notification_body"
            android:textSize="12sp"
            android:maxLines="1" />
    </LinearLayout>
</LinearLayout>
```

#### Expanded view (`notification_expanded.xml`)
```xml
<!-- Similar but with more content: category badge, time, action buttons -->
```

#### Wiring in `NotificationManagerHelper`:
```kotlin
val compactView = RemoteViews(context.packageName, R.layout.notification_compact)
compactView.setTextViewText(R.id.notif_title, title)
compactView.setTextViewText(R.id.notif_body, body)
compactView.setImageViewResource(R.id.notif_icon, iconForCategory(type))

val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded)
expandedView.setTextViewText(R.id.notif_title, title)
expandedView.setTextViewText(R.id.notif_body, body)
// ... set more fields ...

val builder = NotificationCompat.Builder(context, channelId)
    .setSmallIcon(R.drawable.ic_app_mono)
    .setCustomContentView(compactView)
    .setCustomBigContentView(expandedView)
    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setAutoCancel(true)
    .setContentIntent(pendingIntent)
```

### 12.2 BigPictureStyle (for announcements with images)

```kotlin
if (imageUrl != null) {
    // Load image (use Glide/Coil to load bitmap from URL)
    val bitmap = loadBitmapFromUrl(imageUrl)
    builder.setStyle(NotificationCompat.BigPictureStyle()
        .bigPicture(bitmap)
        .setBigContentTitle(title)
        .setSummaryText(category))
}
```

### 12.3 InboxStyle (for grouped messages)

```kotlin
builder.setStyle(NotificationCompat.InboxStyle()
    .setBigContentTitle("New messages")
    .setSummaryText("$count new messages")
    .addLine(message1)
    .addLine(message2)
    .addLine(message3))
```

### 12.4 MessagingStyle (for chat-like notifications)

```kotlin
builder.setStyle(NotificationCompat.MessagingStyle(userDisplayName)
    .addMessage(message1, timestamp1, sender1)
    .addMessage(message2, timestamp2, sender2)
    .setConversationTitle("Class 10-A Parents"))
```

### 12.5 Category-Specific Icons

| Category | Icon Resource | Color Tint |
|---|---|---|
| `attendance` | `@drawable/ic_attendance` | `#7A3F00` (brown) |
| `marks` | `@drawable/ic_marks` | `#3CB9A9` (teal-deep) |
| `homework` | `@drawable/ic_homework` | `#3CB9A9` (teal-deep) |
| `announcement` | `@drawable/ic_announcement` | `#155E3A` (green) |
| `leave` | `@drawable/ic_leave` | `#6B5B95` (purple) |
| `link_request` | `@drawable/ic_link` | `#6B5B95` (purple) |
| `fees` | `@drawable/ic_fees` | `#7A1C18` (dark red) |
| `message` | `@drawable/ic_message` | `#3170C4` (blue) |
| `calendar` | `@drawable/ic_calendar` | `#3170C4` (blue) |
| `general` | `@drawable/ic_app_mono` | default |

**Corner cases**:
- Custom layouts on Android < API 24 → `setCustomContentView` is ignored, falls back to standard template
- RemoteViews text color must use resolved colors (not theme references) — notification views don't have access to the app theme
- Large images in BigPictureStyle → may cause `RemoteViews.RemoteServiceException` if bitmap is too large (keep under 450x450dp / ~200KB)
- Custom layout with dark mode → must provide `night` resource qualifiers or use `@android:color` references

---

## 13. Deep-Link Routing on Tap

### 13.1 Current State

`NotificationManagerHelper.buildContentPendingIntent()` creates an Intent targeting `MainActivity` with:
- `EXTRA_DEEP_LINK` = raw path string (e.g., `"parent/academics/attendance"`)
- Intent data = `vidyaprayag://app/parent/academics/attendance`
- `EXTRA_FROM_PUSH` = true
- `EXTRA_NOTIFICATION_TYPE`, `EXTRA_ENTITY_ID`, `EXTRA_SCHOOL_ID`

**Problem**: `MainActivity` never reads these extras. The notification tap just opens the app to whatever screen was last visible.

### 13.2 Implementation

**File**: `composeApp/src/androidMain/kotlin/com/littlebridge/enrollplus/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Read deep link from notification intent
        deepLink.value = extractDeepLink(intent)
        // ... existing splash/edge-to-edge setup ...
        setContent {
            App(
                onContentRendered = { contentReady.value = true },
                deepLink = deepLink.value,
                onDeepLinkConsumed = { deepLink.value = null }
            )
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = extractDeepLink(intent)
    }
    
    private fun extractDeepLink(intent: Intent): String? {
        if (!intent.getBooleanExtra(NotificationManagerHelper.EXTRA_FROM_PUSH, false)) return null
        return intent.getStringExtra(NotificationManagerHelper.EXTRA_DEEP_LINK)
    }
}
```

**File**: `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/App.kt`

Pass `deepLink` through to `NavGraphV2`, which routes to the correct portal + screen.

**File**: `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/navigation/NavGraphV2.kt`

**Step 1**: Add `deepLink` + `onDeepLinkConsumed` parameters to `NavGraphV2`:
```kotlin
@Composable
fun NavGraphV2(
    role: String?,
    isAuthenticated: Boolean,
    onLogout: () -> Unit,
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val entryRole = EntryRole.from(role)
    val tone = entryRole.tone(isAuthenticated)

    // Parse the deep link once when it arrives.
    var pendingNavigation by remember { mutableStateOf<DeepLinkTarget?>(null) }
    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            pendingNavigation = parseDeepLink(deepLink, entryRole)
            onDeepLinkConsumed()
        }
    }

    VTheme(tone = tone) {
        if (isAuthenticated) {
            AuthedFlow(
                role = entryRole,
                onLogout = onLogout,
                deepLinkTarget = pendingNavigation,
                onDeepLinkNavigated = { pendingNavigation = null },
                modifier = modifier,
            )
        } else {
            UnauthFlow(modifier = modifier)
        }
    }
}
```

**Step 2**: Define the `DeepLinkTarget` sealed class and `parseDeepLink()`:
```kotlin
sealed class DeepLinkTarget {
    abstract val role: EntryRole

    data class ParentTab(override val role: EntryRole, val tab: String, val overlay: String? = null) : DeepLinkTarget()
    data class TeacherScreen(override val role: EntryRole, val screen: String) : DeepLinkTarget()
    data class SchoolScreen(override val role: EntryRole, val screen: String) : DeepLinkTarget()
    data class Generic(override val role: EntryRole, val path: String) : DeepLinkTarget()
}

fun parseDeepLink(path: String, currentRole: EntryRole): DeepLinkTarget {
    val normalized = path.trim().removePrefix("/")
    val segments = normalized.split("/").filter { it.isNotBlank() }
    if (segments.isEmpty()) return DeepLinkTarget.Generic(currentRole, path)

    return when (segments.first()) {
        "parent" -> {
            val tab = segments.getOrNull(1) ?: "home"
            val overlay = when (segments.getOrNull(2)) {
                "leave" -> "leave"
                "messages" -> "messages"
                "notifications" -> "notifications"
                "calendar" -> "calendar"
                else -> null
            }
            DeepLinkTarget.ParentTab(EntryRole.Parent, tab, overlay)
        }
        "teacher" -> DeepLinkTarget.TeacherScreen(EntryRole.Teacher, segments.getOrNull(1) ?: "home")
        "school", "admin" -> DeepLinkTarget.SchoolScreen(EntryRole.SchoolAdmin, segments.getOrNull(1) ?: "home")
        "announcements" -> DeepLinkTarget.Generic(currentRole, path)
        "calendar" -> DeepLinkTarget.Generic(currentRole, path)
        else -> DeepLinkTarget.Generic(currentRole, path)
    }
}
```

**Step 3**: Thread `deepLinkTarget` through `AuthedFlow` → `RolePortal` → each portal. Each portal consumes it to set its initial `tab` / `overlay`:
```kotlin
@Composable
private fun AuthedFlow(
    role: EntryRole,
    onLogout: () -> Unit,
    deepLinkTarget: DeepLinkTarget? = null,
    onDeepLinkNavigated: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // ... existing gate logic ...
    // When route == Portal, pass deepLinkTarget to RolePortal:
    AuthedRoute.Portal -> RolePortal(
        role = role,
        onLogout = onLogout,
        deepLinkTarget = deepLinkTarget,
        onDeepLinkNavigated = onDeepLinkNavigated,
        modifier = modifier,
    )
}

@Composable
private fun RolePortal(
    role: EntryRole,
    onLogout: () -> Unit,
    deepLinkTarget: DeepLinkTarget? = null,
    onDeepLinkNavigated: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (role) {
        EntryRole.SchoolAdmin, EntryRole.SuperAdmin -> SchoolPortalV2(
            onLogout = onLogout,
            deepLinkTarget = deepLinkTarget as? DeepLinkTarget.SchoolScreen,
            onDeepLinkNavigated = onDeepLinkNavigated,
            modifier = modifier,
        )
        EntryRole.Teacher -> TeacherPortalV2(
            onLogout = onLogout,
            deepLinkTarget = deepLinkTarget as? DeepLinkTarget.TeacherScreen,
            onDeepLinkNavigated = onDeepLinkNavigated,
            modifier = modifier,
        )
        EntryRole.Parent -> ParentPortalV2(
            onLogout = onLogout,
            deepLinkTarget = deepLinkTarget as? DeepLinkTarget.ParentTab,
            onDeepLinkNavigated = onDeepLinkNavigated,
            modifier = modifier,
        )
        EntryRole.Unknown -> ParentPortalV2(onLogout = onLogout, modifier = modifier)
    }
}
```

**Step 4**: In `ParentPortalV2`, consume the `deepLinkTarget` to set initial `tab` + `overlay`:
```kotlin
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    deepLinkTarget: DeepLinkTarget.ParentTab? = null,
    onDeepLinkNavigated: () -> Unit = {},
    modifier: Modifier = Modifier,
    // ... existing VM params ...
) {
    var tab by remember { mutableStateOf(deepLinkTarget?.tab ?: "home") }
    var overlay by remember {
        mutableStateOf(
            when (deepLinkTarget?.overlay) {
                "notifications" -> ParentOverlay.Notifications
                "calendar" -> ParentOverlay.Calendar
                "leave" -> ParentOverlay.Leave
                "messages" -> ParentOverlay.Messages
                else -> ParentOverlay.None
            }
        )
    }

    // Consume the deep link once the portal is composed.
    LaunchedEffect(deepLinkTarget) {
        if (deepLinkTarget != null) onDeepLinkNavigated()
    }

    // ... rest of existing ParentPortalV2 body ...
}
```

**Step 5**: In `App.kt`, pass `deepLink` through from `MainActivity`:
```kotlin
@Composable
fun App(
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    // ... existing params ...
) {
    // ... existing session/role resolution ...
    NavGraphV2(
        role = role,
        isAuthenticated = isAuthenticated,
        onLogout = { /* existing logout logic */ },
        deepLink = deepLink,
        onDeepLinkConsumed = onDeepLinkConsumed,
    )
}
```

### 13.3 Deep-Link Path Catalog

| Deep Link Path | Role | Target Screen |
|---|---|---|
| `parent/academics/attendance` | Parent | Academics tab → Attendance |
| `parent/academics/marks` | Parent | Academics tab → Marks |
| `parent/academics` | Parent | Academics tab |
| `parent/fees` | Parent | Fees tab |
| `parent/leave` | Parent | Leave overlay |
| `parent/messages` | Parent | Messages overlay |
| `parent/dashboard` | Parent | Home tab |
| `announcements/{id}` | Any | Announcements detail |
| `calendar/{id}` | Any | Calendar event detail |
| `teacher/leave-requests` | Teacher | Leave requests |
| `school/leave-requests` | School Admin | Leave requests |
| `admin/link-requests` | School Admin | Link requests queue |

### 13.4 Corner Cases

- **User not authenticated when tapping notification**: The deep link should be stashed and replayed after login. Store in `PreferenceRepository` or a singleton.
- **User's role doesn't match the deep link** (e.g., teacher taps a `parent/fees` link): Show an error or redirect to their own portal. The deep link is only a hint, not a guarantee.
- **Deep link points to a deleted entity** (e.g., announcement was deleted): Navigate to the parent screen and show a "content no longer available" message.
- **App is in foreground when notification tapped**: `onNewIntent` fires (not `onCreate`). The `deepLink` state updates and `LaunchedEffect` re-navigates.
- **Multiple notifications tapped in sequence**: Each tap fires a new intent. The previous deep link is consumed before the next one is processed.
- **Deep link path format varies**: Some paths have leading `/` (e.g., `/announcement/123`), some don't (e.g., `parent/fees`). `NotificationManagerHelper.normalizeDeepLinkUri()` already handles this — normalize in the parser too.

---

## 14. Notification Action Buttons

### 14.1 Action Catalog

| Category | Action | Label | Behavior |
|---|---|---|---|
| `leave` | `APPROVE` | "Approve" | Opens app to leave request detail (foreground) |
| `leave` | `REJECT` | "Reject" | Opens app to leave request detail (foreground) |
| `link_request` | `VIEW` | "View" | Opens app to link request queue |
| `fees` | `PAY` | "Pay Now" | Opens app to fees screen |
| `message` | `REPLY` | "Reply" | Opens app to message thread |
| Any | `MARK_READ` | "Mark Read" | Broadcast receiver marks notification read (no UI) |

### 14.2 Implementation (Android)

**BroadcastReceiver for inline actions** (no UI):
```kotlin
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_READ -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                    // Optionally call API to mark read
                }
            }
        }
    }
}
```

**Register in AndroidManifest.xml**:
```xml
<receiver android:name=".notification.NotificationActionReceiver" android:exported="false">
    <intent-filter>
        <action android:name="com.littlebridge.enrollplus.notification.MARK_READ" />
    </intent-filter>
</receiver>
```

**Add actions in `NotificationManagerHelper.displayNotification()`**:
```kotlin
val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
    action = ACTION_MARK_READ
    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
}
val markReadPendingIntent = PendingIntent.getBroadcast(
    context, notificationId, markReadIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
builder.addAction(0, "Mark Read", markReadPendingIntent)
```

### 14.3 Corner Cases

- **Action button on Android < API 24**: Actions are supported but limited to 3. Wear OS may show up to 5.
- **Action triggers API call**: Must be async (BroadcastReceiver has ~10 sec window). Use `goAsync()` or a coroutine launched from `onReceive`.
- **Notification cancelled before action fires**: `PendingIntent` is still valid (it's a snapshot). The action executes but the notification is already gone.
- **Multiple notifications with same action**: Use unique `notificationId` as PendingIntent request code to avoid collisions.
- **iOS action buttons**: Use `UNNotificationCategory` with `UNNotificationAction`. Limited to 4 actions per category.

---

## 15. Notification Grouping / Stacking

### 15.1 Android Grouping

Use `setGroup()` to stack related notifications:

```kotlin
// Per-notification:
builder.setGroup("attendance_${parentId}")  // group key

// Summary notification (posted last):
val summaryBuilder = NotificationCompat.Builder(context, channelId)
    .setSmallIcon(R.drawable.ic_app_mono)
    .setContentTitle("New attendance alerts")
    .setContentText("$count attendance updates")
    .setStyle(NotificationCompat.InboxStyle()
        .setBigContentTitle("New attendance alerts")
        .setSummaryText("$count updates"))
    .setGroup("attendance_${parentId}")
    .setGroupSummary(true)
```

### 15.2 Grouping Strategy

| Group Key | When | Summary Text |
|---|---|---|
| `attendance_{parentId}` | Multiple children absent same day | "N attendance updates" |
| `messages_{threadId}` | Multiple messages in same thread | "N new messages" |
| `fees_{parentId}` | Multiple fees due/overdue | "N fee reminders" |
| `announcements_{schoolId}` | Multiple announcements from same school | "N announcements" |

### 15.3 Corner Cases

- **Summary notification must be posted AFTER all children**: Android collapses children under the summary. If summary is posted first, children appear individually until summary is posted.
- **Notification ID for summary**: Use a stable derived ID (e.g., group key hashCode) so re-posting updates the summary.
- **User dismisses summary**: All children are dismissed too (Android behavior).
- **Android < API 24**: Grouping is ignored; notifications appear individually.
- **Do not group across categories**: A `fees` notification and an `attendance` notification should NOT be in the same group — they use different channels.

---

## 16. Scheduled Notifications

### 16.1 Server-Side Scheduler

**New file**: `server/src/main/kotlin/com/littlebridge/enrollplus/feature/notifications/NotificationScheduler.kt`

**Jobs**:

| Job | Frequency | Query | Action |
|---|---|---|---|
| Fee due reminder | Every 1 hour | `fee_records WHERE status='DUE' AND due_date <= now()+3days AND (last_reminded_at IS NULL OR last_reminded_at < now()-24h)` | `Notify.toUser()` per fee |
| Fee overdue reminder | Every 1 hour | `fee_records WHERE status='OVERDUE' AND (last_reminded_at IS NULL OR last_reminded_at < now()-7days)` | `Notify.toUser()` per fee |
| Calendar event reminder | Every 1 hour | `calendar_events WHERE start_time BETWEEN now() AND now()+24h AND reminder_sent=false` | `Notify.toUsers()` per event, then set `reminder_sent=true` |

### 16.2 DB Migrations Needed

**`fee_records` table**:
```sql
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS last_reminded_at timestamp NULL;
```

**`calendar_events` table**:
```sql
ALTER TABLE calendar_events ADD COLUMN IF NOT EXISTS reminder_sent boolean NOT NULL DEFAULT false;
```

### 16.3 Client-Side Local Scheduling (Optional)

For reminders that should fire even when the server is down or the app is offline:

**Android**: Use `AlarmManager` or `WorkManager` for local scheduled notifications.

```kotlin
// Schedule a local notification 1 hour before a calendar event
val intent = Intent(context, LocalNotificationReceiver::class.java)
intent.putExtra("title", "Upcoming: $eventTitle")
intent.putExtra("body", "Starts in 1 hour")
val pendingIntent = PendingIntent.getBroadcast(context, eventId.hashCode(), intent, FLAG_IMMUTABLE)
val alarmManager = context.getSystemService(AlarmManager::class.java)
alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, eventStartTime - 3600000, pendingIntent)
```

**Corner cases**:
- App uninstalled → all scheduled alarms are cancelled automatically
- Device reboot → alarms are lost (need `BOOT_COMPLETED` receiver to reschedule)
- Doze mode → `setExactAndAllowWhileIdle` fires during doze but may be deferred
- Battery saver → may delay alarms
- Multiple devices → each device schedules its own local notification (could cause duplicate if server also pushes)

---

## 17. Notification Preferences Screen

### 17.1 What to Build

A screen accessible from the "Notification preferences" footer in `NotificationsScreenV2` (currently a no-op).

**Sections**:

1. **Per-category toggles**: Allow users to enable/disable notifications per category
   - Attendance alerts (on/off)
   - Academic updates (on/off)
   - Announcements (on/off)
   - Leave requests (on/off)
   - Fee reminders (on/off)
   - Messages (on/off)
   - Calendar events (on/off)

2. **Sound preferences**: Per-category sound selection (or "No sound")
   - This maps to Android notification channel settings — can deep-link to `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS`

3. **Quiet hours**: Time range during which notifications are silent
   - Client-side: suppress local display during quiet hours
   - Server-side: defer push to after quiet hours (advanced — not MVP)

4. **Digest mode**: Batch notifications into a daily summary instead of individual pushes
   - Server-side: hold notifications and send one digest push at a set time

### 17.2 Implementation

**New screen**: `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/screens/notifications/NotificationPreferencesScreenV2.kt`

**New ViewModel**: `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/notification/presentation/NotificationPreferencesViewModel.kt`

**New API**: `GET /api/v1/notification-preferences`, `PUT /api/v1/notification-preferences`

**API Contracts**:

`GET /api/v1/notification-preferences`
```json
// Response 200:
{
  "preferences": [
    {
      "category": "attendance",
      "enabled": true,
      "sound": "default"
    },
    {
      "category": "fees",
      "enabled": true,
      "sound": "fee_alert"
    },
    {
      "category": "announcements",
      "enabled": false,
      "sound": "default"
    }
  ]
}
```

`PUT /api/v1/notification-preferences`
```json
// Request body:
{
  "preferences": [
    {
      "category": "attendance",
      "enabled": true,
      "sound": "default"
    },
    {
      "category": "announcements",
      "enabled": false
    }
  ]
}

// Response 200:
{
  "message": "Preferences updated"
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT | `{"error": "Invalid token"}` |
| 400 | Invalid category name | `{"error": "Invalid category: xyz"}` |
| 400 | Malformed JSON | `{"error": "Bad request"}` |

**Server routing** — `NotificationPreferencesRouting.kt`:
```kotlin
fun Route.notificationPreferencesRoutes() {
    authenticate("jwt") {
        get("/api/v1/notification-preferences") {
            val userId = call.principalUserId()!!
            val prefs = dbQuery {
                NotificationPreferencesTable.selectAll()
                    .where { NotificationPreferencesTable.userId eq userId }
                    .map {
                        NotificationPreferenceDto(
                            category = it[NotificationPreferencesTable.category],
                            enabled = it[NotificationPreferencesTable.enabled],
                            sound = it[NotificationPreferencesTable.sound]
                        )
                    }
            }
            // Fill in defaults for categories not in DB
            val allCategories = listOf(
                "attendance", "marks", "homework", "announcements",
                "leave", "fees", "link_requests", "messages", "calendar"
            )
            val existingCats = prefs.map { it.category }.toSet()
            val complete = prefs + allCategories.filter { it !in existingCats }.map {
                NotificationPreferenceDto(it, true, "default")
            }
            call.respond(mapOf("preferences" to complete))
        }

        put("/api/v1/notification-preferences") {
            val userId = call.principalUserId()!!
            val req = call.receive<UpdatePreferencesRequest>()
            dbQuery {
                for (pref in req.preferences) {
                    NotificationPreferencesTable.upsert(
                        NotificationPreferencesTable.userId,
                        NotificationPreferencesTable.category
                    ) {
                        it[NotificationPreferencesTable.userId] = userId
                        it[NotificationPreferencesTable.category] = pref.category
                        it[NotificationPreferencesTable.enabled] = pref.enabled
                        pref.sound?.let { s -> it[NotificationPreferencesTable.sound] = s }
                        it[NotificationPreferencesTable.updatedAt] = Instant.now()
                    }
                }
            }
            call.respond(mapOf("message" to "Preferences updated"))
        }
    }
}
```

**DTOs**:
```kotlin
@Serializable
data class NotificationPreferenceDto(
    val category: String,
    val enabled: Boolean,
    val sound: String? = "default"
)

@Serializable
data class UpdatePreferencesRequest(
    val preferences: List<NotificationPreferenceDto>
)
```

**New DB table**:
```sql
CREATE TABLE IF NOT EXISTS notification_preferences (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    category varchar(32) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    sound varchar(64) NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    UNIQUE(user_id, category)
);
```

**Corner cases**:
- User disables a category on the server → `Notify.toUsers()` should check preferences before inserting. This adds a DB read per notification trigger.
- User disables a channel in Android Settings → app-level toggle has no effect (Android Settings wins). Show a "Open system settings" button.
- Quiet hours + urgent notification (absent child) → should it override quiet hours? Configurable per category.
- New user → default all categories to enabled.
- Preferences synced across devices → stored server-side, not just local.

**Exact code** — add preferences filtering to `Notify.toUsers()`:
```kotlin
suspend fun toUsers(
    userIds: Collection<UUID>,
    category: String,
    title: String,
    body: String = "",
    schoolId: UUID? = null,
    actorId: UUID? = null,
    deepLink: String? = null,
    refType: String? = null,
    refId: String? = null,
) {
    val recipients = userIds.distinct()
    if (recipients.isEmpty()) return

    // Filter out users who have disabled this category.
    // OTP notifications bypass preferences (always sent).
    val filteredRecipients = if (category == "otp") {
        recipients
    } else {
        val disabledUserIds = dbQuery {
            NotificationPreferencesTable.selectAll()
                .where {
                    (NotificationPreferencesTable.userId inList recipients) and
                    (NotificationPreferencesTable.category eq category) and
                    (NotificationPreferencesTable.enabled eq false)
                }
                .map { it[NotificationPreferencesTable.userId] }
                .toSet()
        }
        recipients.filter { it !in disabledUserIds }
    }

    if (filteredRecipients.isEmpty()) return

    val now = Instant.now()
    dbQuery {
        NotificationsTable.batchInsert(filteredRecipients) { uid ->
            this[NotificationsTable.userId] = uid
            this[NotificationsTable.schoolId] = schoolId
            this[NotificationsTable.category] = category
            this[NotificationsTable.title] = title
            this[NotificationsTable.body] = body
            this[NotificationsTable.deepLink] = deepLink
            this[NotificationsTable.actorId] = actorId
            this[NotificationsTable.refType] = refType
            this[NotificationsTable.refId] = refId
            this[NotificationsTable.isRead] = false
            this[NotificationsTable.createdAt] = now
        }
    }

    // Push bridge (§6.1) — fire-and-forget
    runCatching {
        pushService.send(
            userIds = filteredRecipients,
            category = category,
            title = title,
            body = body,
            deepLink = deepLink,
        )
    }
}
```

---

## 18. Corner Cases & Edge Scenarios

### 18.1 Multi-Device

| Scenario | Behavior |
|---|---|
| User has phone + tablet | Both receive the FCM push (multi-device fan-out in `NotificationService`) |
| User reads notification on phone | `PATCH /notifications/{id}/read` marks the DB row read. Tablet's inbox refresh shows it as read. But the system tray notification on the tablet is NOT auto-dismissed (FCM doesn't sync read state across devices). |
| User uninstalls app on one device | FCM reports token as UNREGISTERED on next push → `handleFailure()` deactivates the token. Other device continues receiving. |
| User logs out on one device | `DeviceTokenRepository.deactivateAllForUser()` is called → ALL tokens for that user are deactivated, including the other device that's still logged in. **Bug**: should only deactivate the current device's token, not all. |

**Fix for logout bug**: Pass the specific FCM token to deactivate, not the user ID.

### 18.2 Token Lifecycle

| Scenario | Behavior |
|---|---|
| App first install | `DeviceTokenRegistrar.syncRegistration()` fetches FCM token, registers with backend |
| App update (no data clear) | FCM token unchanged → `syncRegistration()` compares cached, skips re-registration |
| App data cleared | FCM token rotated → `onNewToken()` fires → `syncRegistration()` registers new token. Old token remains in DB as active until next push fails. |
| User logs in | `MainViewModel` triggers `syncDeviceToken()` when `userToken` becomes non-null |
| User logs out | `DeviceTokenRepository.deactivateAllForUser()` called — **should be `deactivateToken(currentToken)` instead** |
| User logs in with different account on same device | FCM token may be the same → `upsertToken()` re-points owner to new user. Old user no longer receives pushes on this device. |
| Firebase not configured (dev) | `FirebaseApp.getApps()` empty → `syncRegistration()` skips silently. No crash. |
| No internet during registration | API call fails → token NOT cached → next launch retries. |

### 18.3 Permission Scenarios

| Scenario | Behavior |
|---|---|
| Android 13+, first launch | `PermissionViewModel.checkNotificationPermission()` triggers system dialog |
| User grants | Notifications display normally |
| User denies once | `shouldShowRationale()` returns true → custom rationale dialog shown on next launch |
| User denies twice (permanently) | `shouldShowRationale()` returns false → "Not Now" persisted in prefs → rationale never shown again |
| User denies then changes mind | Must go to Android Settings → App → Notifications. App cannot re-prompt. |
| Android < 13 | Permission not required → `hasPermission()` returns true |
| User grants but disables channel in Settings | `areNotificationsEnabled()` returns true but channel is blocked. Notification is silently dropped. |

### 18.4 Payload Edge Cases

| Scenario | Behavior |
|---|---|
| Empty FCM payload (no data, no notification) | `onMessageReceived` logs and returns. No notification displayed. |
| Data payload with no title/body | `onMessageReceived` logs warning and returns. No notification displayed. |
| Notification-only payload, app in background | System tray auto-renders. `onMessageReceived` NOT called. No deep-link routing. |
| Notification-only payload, app in foreground | `onMessageReceived` IS called. Falls back to notification block for title/body. |
| Data payload with title/body but no `type` | `type` is null → `channelIdForCategory(null)` → `general_notifications` channel. |
| Very long body text | `BigTextStyle` handles up to ~500 chars. Beyond that, truncated with ellipsis. |
| Special characters in title/body | FCM handles UTF-8. No encoding issues. |
| `deepLink` is a full URL (not in-app path) | `normalizeDeepLinkUri()` detects scheme and returns as-is. PendingIntent carries the full URL. |

### 18.5 Scale & Performance

| Scenario | Behavior |
|---|---|
| 1000 recipients, all have device tokens | `NotificationService.send()` chunks at 500 tokens per multicast. 2 FCM API calls. ~2-5 seconds total. |
| 1000 recipients, none have device tokens | `activeTokensForUsers()` returns empty → `send()` returns `sent=0`. No FCM calls. Fast. |
| 5000 recipients (whole school) | 10 multicast batches. ~10-25 seconds. Should be async (don't block the HTTP request). |
| FCM quota exceeded | `sendEachForMulticast` throws → batch counted as failed → tokens left active for retry. |
| FCM service outage | All batches fail → all tokens left active → in-app notifications still delivered. |
| Server restart during fan-out | Some recipients get the push, some don't. In-app rows may be partially inserted. Acceptable (best-effort). |

### 18.6 Data Consistency

| Scenario | Behavior |
|---|---|
| `Notify.toUsers()` succeeds (DB insert) but `NotificationService.send()` fails | In-app notification exists, no push. User sees it when they open the app. Correct. |
| `Notify.toUsers()` fails (DB error) | No in-app notification, no push. The originating HTTP request fails. Caller sees the error. |
| `NotificationService.send()` succeeds but some tokens are invalid | Invalid tokens deactivated. Valid tokens receive push. Tallies returned. |
| Duplicate `Notify.toUsers()` calls for same event | Multiple in-app rows created (no dedup). Multiple pushes sent. **Should add idempotency key**. |

### 18.7 Security

| Scenario | Behavior |
|---|---|
| User tries to register another user's device token | `POST /api/device-tokens` is JWT-authenticated. The token is registered under `jwt.sub` (the caller). Cannot register for another user. |
| Admin broadcasts to users outside their school | `requireSchoolAdmin()` guards the endpoint. But `SendNotificationRequest.userIds` is not school-scoped — an admin could pass any userId. **Should validate recipients belong to admin's school.** |
| Malicious FCM payload (XSS in title/body) | System tray renders as plain text. No HTML interpretation. Safe. |
| Deep link injection | Deep link path is server-generated. A malicious server could send `../../` paths. Client parser must validate/sanitize. |

### 18.8 iOS-Specific

| Scenario | Behavior |
|---|---|
| APNs token rotation | Firebase handles via `onNewToken`. Same flow as Android. |
| iOS silent push (content-available) | Delivered but no UI. Use for background sync. |
| iOS critical alert | Requires Apple entitlement. Not MVP. |
| iOS provisional authorization | Notifications appear in Notification Center without sound/badge. User can promote to prominent. |
| App killed (swiped away) | APNs delivers push. System displays notification. App is NOT launched (unless user taps). |

---

## 19. Implementation Roadmap

### Phase 1: Bridge + Token Fixes (Highest Impact) — 2-3 days

1. **Bridge `Notify.toUsers()` → `NotificationService.send()`** in `Notify.kt` (§6.1)
   - Wrap in `runCatching`
   - Pass `category` as `data["type"]` in the FCM payload
   - Test: trigger attendance, verify parent receives both in-app + push

2. **Fix logout FCM token cache clearing** (§6.2.1)
   - Clear `prefs.setFcmToken(null)` in `AuthRepositoryImpl.logout()` after `clearSession()`
   - Ensures re-login with different account triggers re-registration

3. **Fix server-side logout token deactivation** (§6.2.2)
   - Add `fcmToken` to `LogoutDto` and `LogoutRequest`
   - Call `deactivateToken(fcmToken)` in the logout handler
   - Update `AuthApi.logout()` to send FCM token

4. **Remove duplicate device-token endpoint** (§6.3)
   - Delete `POST /api/v1/notifications/device-token` from `NotificationsRouting.kt`

5. **Fix admin broadcast school-scoping** (§6.6)
   - Validate `req.userIds` belong to the admin's school before dispatch

### Phase 2: Channels & Sounds — 2-3 days

6. **Create per-category channels** in `NotificationManagerHelper`
   - 10 channels (see §10)
   - Update `EnRollPlusApp.onCreate()` to call `createAllChannels()`
   - Update `displayNotification()` to use `channelIdForCategory(type)`

7. **Add sound resources** to `res/raw/`
   - 6 custom sounds (see §11)
   - Wire `setSound()` per channel

8. **Add category-specific icons** to `res/drawable/`
   - 10 icons (see §12.5)

### Phase 3: Deep-Link Routing — 1-2 days

9. **Read notification extras in `MainActivity`**
   - Extract `EXTRA_DEEP_LINK` from intent
   - Handle `onNewIntent` for foreground taps

10. **Route deep links in `NavGraphV2` / portal screens**
    - Parse path → role + screen
    - Navigate to correct tab/overlay
    - Handle role mismatch (teacher taps parent link)

### Phase 4: Custom Views & Actions — 2-3 days

11. **Create custom notification layouts** (`notification_compact.xml`, `notification_expanded.xml`)
    - Wire `RemoteViews` in `NotificationManagerHelper`
    - Category-tinted icon tiles

12. **Add notification action buttons**
    - `NotificationActionReceiver` for "Mark Read"
    - "Approve"/"Reject" for leave requests (opens app)
    - "Pay Now" for fees (opens app)

### Phase 5: Scheduled Notifications — 2-3 days

13. **Server-side scheduler** (`NotificationScheduler.kt`)
    - Fee due reminders (3-day window)
    - Fee overdue reminders (weekly)
    - Calendar event reminders (24-hour window)

14. **DB migrations**
    - `fee_records.last_reminded_at` (see §7.1)
    - `calendar_events.reminder_sent` (see §7.2)

### Phase 6: Preferences & Polish — 2-3 days

15. **Notification preferences screen**
    - Per-category toggles
    - "Open system settings" deep-link
    - Server-side preferences table (see §7.3)

16. **Notification grouping**
    - Group key per category + entity
    - Summary notifications

### Phase 7: iOS — 3-5 days

17. **`IOSNotificationService`** implementation
18. **iOS notification categories** with action buttons
19. **iOS sound resources** (.wav format)

### Phase 8: Hardening — 1-2 days

20. **Idempotency keys** for `Notify.toUsers()` to prevent duplicates (see §7.4)
21. **Notification analytics** — track open rates, dismiss rates (optional)

---

## 20. API Contracts

### 20.1 `POST /api/device-tokens` — Register Device Token

**Auth**: JWT required (`authenticate("jwt")`)

**Request body**:
```json
{
  "token": "string (required, non-empty)",
  "platform": "android | ios | web",
  "appVersion": "string (optional)",
  "deviceModel": "string (optional)"
}
```

**Response 200**:
```json
{
  "success": true
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT or invalid JWT | `{"error": "Invalid token"}` |
| 400 | `token` is blank or missing | `{"error": "Bad request"}` |
| 400 | Malformed JSON body | `{"error": "Bad request"}` |

**Validation rules**:
- `token` — required, non-empty string. Must be a valid FCM token.
- `platform` — defaults to `"android"` if not provided. Must be one of `android`, `ios`, `web`.
- `appVersion` — optional, max 64 chars.
- `deviceModel` — optional, max 128 chars.
- `schoolId` is NOT in the request — resolved server-side from `app_users.school_id`.

**Idempotency**: Re-registering the same token updates metadata (`appVersion`, `deviceModel`, `lastSeenAt`, `isActive=true`) and re-points `userId` to the caller. No duplicate rows.

---

### 20.2 `GET /api/v1/notifications` — List Inbox

**Auth**: JWT required

**Query parameters**: None (returns latest 200 for `jwt.sub`)

**Response 200**:
```json
{
  "notifications": [
    {
      "id": "uuid-string",
      "category": "attendance",
      "title": "Attendance update",
      "body": "Your child was marked absent...",
      "time": "2026-06-27T10:30:00Z",
      "unread": true
    }
  ],
  "unread_count": 5
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT or invalid JWT | `{"error": "Invalid token"}` |

**Notes**:
- Returns max 200 items, sorted by `created_at DESC`.
- For parents: merges synth items (announcements + DUE/OVERDUE fees) with real notification rows.
- Synth items have IDs prefixed `ann_` or `fee_` and are always `unread=true`.
- No pagination — future enhancement: cursor-based with `?before=<timestamp>`.

---

### 20.3 `GET /api/v1/notifications/summary` — Unread Count

**Auth**: JWT required

**Response 200**:
```json
{
  "unread_count": 5
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT or invalid JWT | `{"error": "Invalid token"}` |

**Notes**:
- For parents: includes synth items in the count (announcements + DUE/OVERDUE fees).
- Used by the bell icon badge in portal headers.

---

### 20.4 `PATCH /api/v1/notifications/{id}/read` — Mark One Read

**Auth**: JWT required

**Path parameter**: `id` — UUID of the notification

**Response 200**:
```json
{
  "message": "Marked read"
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT or invalid JWT | `{"error": "Invalid token"}` |
| 400 | `id` is not a valid UUID | `{"error": "Bad request"}` |
| 404 | Notification not found or doesn't belong to caller | (no explicit 404 — update affects 0 rows, still returns 200) |

**Security**: The `WHERE` clause includes `userId = jwt.sub` — a user cannot mark another user's notification as read.

**Notes**:
- Synth items (`ann_*`, `fee_*`) cannot be marked read — the UUID parse fails, returns 400.
- Sets `is_read = true` and `read_at = now()`.

---

### 20.5 `POST /api/v1/notifications/read-all` — Mark All Read

**Auth**: JWT required

**Request body**: Empty

**Response 200**:
```json
{
  "message": "All marked read"
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT or invalid JWT | `{"error": "Invalid token"}` |

**Security**: Only updates rows where `userId = jwt.sub AND is_read = false`.

---

### 20.6 `POST /api/admin/notifications/send` — Admin Broadcast

**Auth**: JWT required + `requireSchoolAdmin()` (caller must be `school_admin` or `admin` role)

**Request body**:
```json
{
  "title": "string (required)",
  "body": "string (required)",
  "userIds": ["uuid-string", ...],
  "deepLink": "string (optional)",
  "data": {
    "type": "announcement",
    "entityId": "ANN-8"
  }
}
```

**Response 200**:
```json
{
  "success": true,
  "sentCount": 45,
  "failedCount": 2
}
```

**Error responses**:
| Status | Condition | Body |
|---|---|---|
| 401 | No JWT | `{"error": "Invalid token"}` |
| 403 | Not a school admin | `{"error": "Forbidden"}` |

**Validation rules**:
- `title` — required, non-empty.
- `body` — required (can be empty string).
- `userIds` — list of UUID strings. Empty list → returns 200 with `sentCount=0` (not an error).
- `deepLink` — optional string.
- `data` — optional map of string key-value pairs. Merged into FCM data payload.

**Security gap (§6.6)**: `userIds` are NOT currently validated against the admin's school. Fix documented in §6.6.

**Versioning**: All notification endpoints are under `/api/v1/` (inbox) or `/api/` (device tokens, admin send). No version negotiation. Breaking changes would require a new `/api/v2/` prefix.

---

## 21. Testing Requirements

### 21.1 Unit Testing

| Component | Test Cases | File Location |
|---|---|---|
| `Notify.toUsers()` | Empty recipients → no-op. Single recipient → 1 row inserted. Duplicate recipients → deduped. DB error → exception propagated. | `server/src/test/.../NotifyTest.kt` (to create) |
| `Notify.toUser()` | Delegates to `toUsers(listOf(userId))` — verify single row. | Same as above |
| `NotificationService.send()` | Empty userIds → `sent=0`. Firebase unavailable → `success=false, sent=0`. All tokens valid → `sentCount=N`. Some invalid → `failedCount` reflects, invalid tokens deactivated. Chunk boundary (501 tokens → 2 chunks). | `server/src/test/.../NotificationServiceTest.kt` (to create) |
| `DeviceTokenRepository.upsertToken()` | New token → INSERT. Existing token → UPDATE + re-point userId. Existing token same user → UPDATE metadata only. | `server/src/test/.../DeviceTokenRepositoryTest.kt` (to create) |
| `DeviceTokenRepository.deactivateToken()` | Active token → `is_active=false`. Already inactive → 0 rows. Non-existent → 0 rows. | Same as above |
| `DeviceTokenRepository.activeTokensForUser()` | Multiple active tokens → all returned. Mix of active/inactive → only active. No tokens → empty list. | Same as above |
| `NotificationManagerHelper.channelIdForCategory()` | Each category → correct channel ID. Unknown/null → `general_notifications`. | `composeApp/src/test/...` (to create) |
| `DeviceTokenRegistrar.syncRegistration()` | Firebase not initialized → skip. Not authenticated → skip. Token unchanged → skip. Token changed → POST to API. API failure → token not cached. | `shared/src/test/...` (to create) |

### 21.2 Integration Testing

| Scenario | Steps | Expected Result |
|---|---|---|
| End-to-end push: attendance | 1. Teacher marks attendance via API. 2. `Notify.toUsers()` fires. 3. Check `notifications` table for parent row. 4. Check FCM mock received message. | Parent has in-app notification + FCM push dispatched |
| End-to-end push: announcement | 1. Admin posts announcement. 2. Check notifications for all parents + teachers. 3. Verify FCM fan-out. | All recipients have notification rows + push dispatched |
| Token registration + push | 1. Register device token via `POST /api/device-tokens`. 2. Trigger notification. 3. Verify FCM message sent to registered token. | Token in DB, push delivered to correct token |
| Token deactivation on invalid | 1. Register token. 2. Send push with mock returning UNREGISTERED. 3. Check token `is_active=false`. 4. Send another push → token not included. | Invalid token deactivated, excluded from future sends |
| Logout token cleanup | 1. Login user A, register token. 2. Logout with `fcmToken`. 3. Check token `is_active=false`. 4. Login user B on same device. 5. Verify new token registration. | User A's token deactivated, User B gets new registration |
| Multi-device fan-out | 1. Register 2 tokens for same user. 2. Trigger notification. 3. Verify FCM sends to both tokens. | Both tokens receive the push |
| Rate limiting | 1. Send 51 notifications to same user in one day. 2. Check 51st is skipped. | Rate limit enforced (when implemented) |

### 21.3 End-to-End / Manual Testing Scenarios

| Scenario | Steps | Expected |
|---|---|---|
| Parent receives attendance push | 1. Teacher marks child absent. 2. Parent device receives FCM. 3. System tray shows notification in `attendance_alerts` channel. 4. Tap opens app → attendance screen. | Push delivered, correct channel, deep link works |
| Parent receives fee reminder | 1. Scheduled job runs. 2. Fee is DUE within 3 days. 3. Parent gets push + in-app. 4. Tap opens fees screen. | Scheduled job fires, push delivered |
| Teacher receives leave request | 1. Parent submits leave. 2. Teacher gets push. 3. Tap opens leave requests. | Push delivered, deep link to teacher portal |
| Admin receives link request | 1. Parent requests child link. 2. Admin gets push. 3. Tap opens link requests queue. | Push delivered, deep link to admin portal |
| Notification permission denied | 1. Deny permission on Android 13+. 2. Trigger notification. 3. No system tray notification. 4. In-app notification still in inbox. | Permission respected, in-app still works |
| App killed, notification received | 1. Kill app. 2. Trigger notification. 3. System tray shows notification. 4. Tap opens app to deep link. | Background delivery works |
| Multiple notifications grouped | 1. Trigger 3 attendance notifications for same parent. 2. System tray shows grouped notification. 3. Expand → see 3 items. | Grouping works (when implemented) |

### 21.4 Edge Cases & Failure Scenarios

| Scenario | Expected Behaviour |
|---|---|
| Firebase credentials missing | `NotificationService.send()` returns `success=false, sent=0`. No crash. In-app notification still delivered. |
| FCM quota exceeded | Batch fails, all tokens in chunk counted as failed, left active. `println` logs the error. |
| DB connection drops during `Notify.toUsers()` | `dbQuery` throws, originating HTTP request fails. No push sent. |
| DB connection drops during `NotificationService.send()` | In-app row already inserted. Push fails silently (`runCatching`). |
| Network failure during token registration | API call fails, token NOT cached in prefs. Next launch retries. |
| User has 0 device tokens | `activeTokensForUsers()` returns empty. `send()` returns `sent=0`. In-app notification still delivered. |
| Very long title (>100 chars) | FCM truncates at ~1000 chars. System tray may truncate further. No crash. |
| Special characters in title/body | UTF-8 handled by FCM. No encoding issues. |
| Concurrent `Notify.toUsers()` calls for same event | Multiple rows inserted (no dedup without idempotency key). Multiple pushes sent. |
| Server restart during scheduled job | Job resumes on next tick (idempotent — queries are time-based). |

---

## 22. Sequence Diagrams

### 22.1 Notification Creation (Trigger → DB → Push)

```
Teacher App          Server (Ktor)           Database           Firebase
     │                    │                     │                  │
     │  POST /attendance  │                     │                  │
     │───────────────────>│                     │                  │
     │                    │  Mark absent        │                  │
     │                    │  in DB              │                  │
     │                    │────────────────────>│                  │
     │                    │                     │                  │
     │                    │  Notify.toUsers()   │                  │
     │                    │  batchInsert        │                  │
     │                    │────────────────────>│                  │
     │                    │  (notifications     │                  │
     │                    │   row created)      │                  │
     │                    │                     │                  │
     │                    │  pushService.send() │                  │
     │                    │  (runCatching)      │                  │
     │                    │  resolve tokens     │                  │
     │                    │────────────────────>│                  │
     │                    │  <─ tokens ─────────│                  │
     │                    │                     │                  │
     │                    │  sendEachForMulticast                   │
     │                    │─────────────────────────────────────────>│
     │                    │                     │                  │
     │  200 OK            │                     │                  │
     │<───────────────────│                     │                  │
     │                    │                     │                  │
     │                    │  (async: per-token  │                  │
     │                    │   failure →         │                  │
     │                    │   deactivateToken)  │                  │
     │                    │────────────────────>│                  │
```

### 22.2 FCM Delivery (Server → Device)

```
Server                Firebase Cloud          Android Device
  │                        │                        │
  │  sendEachForMulticast  │                        │
  │  (500 tokens/chunk)    │                        │
  │───────────────────────>│                        │
  │                        │  FCM push              │
  │                        │  (notification + data) │
  │                        │───────────────────────>│
  │                        │                        │
  │                        │           App in foreground?
  │                        │              │         │
  │                        │         Yes  │  No    │
  │                        │              │         │
  │                        │              ▼         ▼
  │                        │     onMessageReceived  System tray
  │                        │     extract data       auto-renders
  │                        │     displayNotification notification
  │                        │     (channel, icon)    block
  │                        │                        │
  │  <─ batch response ────│                        │
  │  (per-token results)   │                        │
  │                        │                        │
  │  deactivate invalid    │                        │
  │  tokens                │                        │
```

### 22.3 User Tap Flow (Notification → App → Screen)

```
Android Device          MainActivity          NavGraphV2          Target Screen
     │                      │                    │                    │
     │  User taps           │                    │                    │
     │  notification        │                    │                    │
     │─────────────────────>│                    │                    │
     │                      │  extractDeepLink() │                    │
     │                      │  (EXTRA_DEEP_LINK) │                    │
     │                      │                    │                    │
     │                      │  setContent {      │                    │
     │                      │    App(deepLink)   │                    │
     │                      │  }                 │                    │
     │                      │───────────────────>│                    │
     │                      │                    │  LaunchedEffect    │
     │                      │                    │  parse path        │
     │                      │                    │  navigate()        │
     │                      │                    │───────────────────>│
     │                      │                    │                    │
     │                      │                    │  onDeepLinkConsumed│
     │                      │                    │  (clear state)     │
     │                      │                    │                    │
```

### 22.4 Device Token Registration Flow

```
Android Device          Server                Database
     │                      │                    │
     │  FirebaseMessaging   │                    │
     │  .getInstance().token│                    │
     │  (fetch FCM token)   │                    │
     │                      │                    │
     │  Compare with        │                    │
     │  cached token        │                    │
     │  (prefs.getFcmToken) │                    │
     │                      │                    │
     │  Changed? ──No──> Skip (no-op)            │
     │     │                                      │
     │    Yes                                     │
     │     │                                      │
     │  POST /api/device-tokens                   │
     │  {token, platform, appVersion, deviceModel}│
     │─────────────────────>│                    │
     │                      │  resolveSchoolId   │
     │                      │  (app_users)       │
     │                      │───────────────────>│
     │                      │  <─ schoolId ──────│
     │                      │                    │
     │                      │  upsertToken       │
     │                      │  (INSERT or UPDATE)│
     │                      │───────────────────>│
     │                      │                    │
     │  200 {success:true}  │                    │
     │<─────────────────────│                    │
     │                      │                    │
     │  Cache token         │                    │
     │  prefs.setFcmToken() │                    │
```

### 22.5 Backend Acknowledgement (Mark Read)

```
Android Device          Server                Database
     │                      │                    │
     │  User taps notif     │                    │
     │  in inbox            │                    │
     │                      │                    │
     │  PATCH /api/v1/      │                    │
     │  notifications/{id}  │                    │
     │  /read               │                    │
     │─────────────────────>│                    │
     │                      │  WHERE userId =    │
     │                      │  jwt.sub AND id=?  │
     │                      │  SET is_read=true, │                    │
     │                      │  read_at=now()     │                    │
     │                      │───────────────────>│
     │                      │                    │
     │  200 {message:       │                    │
     │   "Marked read"}     │                    │
     │<─────────────────────│                    │
```

---

## 23. Configuration

### 23.1 Environment Variables

| Variable | Required | Description | Default |
|---|---|---|---|
| `FIREBASE_CREDENTIALS_JSON` | No* | Full service-account JSON for the primary Firebase project (Enroll/VidyaPrayag app) | — |
| `FIREBASE_CREDENTIALS_FILE` | No* | Absolute path to service-account JSON file on disk | — |
| `GOOGLE_APPLICATION_CREDENTIALS` | No* | Google ADC convention path to credentials file | — |
| `OTP_SENDER_FIREBASE_CREDENTIALS_JSON` | No | Full service-account JSON for the OTPSender Firebase project | — |
| `OTP_SENDER_FIREBASE_CREDENTIALS_FILE` | No | Path to OTPSender service-account JSON file | — |

*At least one of the first three must be set for push notifications to work. If none are set, the server boots normally but push is disabled (graceful degradation).

### 23.2 Firebase Configuration

**Server-side (Firebase Admin SDK)**:
- Initialized in `FirebaseAdminInitializer.kt`
- Primary app name: `vidyaprayag-server`
- OTPSender app name: `vidyaprayag-otpsender`
- Credentials resolved via env vars → local.properties → ADC (see §6.8 for full resolution chain)

**Client-side (Android)**:
- `google-services.json` in `composeApp/` module root
- `FirebaseApp` auto-initialized by `com.google.gms.google-services` Gradle plugin
- `FirebaseMessaging.getInstance().token` fetches the FCM registration token

**Client-side (iOS)**:
- `GoogleService-Info.plist` in `iosApp/` target
- `FirebaseMessaging` auto-initialized
- APNs token fetched via `FirebaseMessaging.instance.token()`

### 23.3 Server Configuration

| Config | Location | Description |
|---|---|---|
| Database URL | `DATABASE_URL` env var or `local.properties` | PostgreSQL connection string |
| JWT secret | `JWT_SECRET` env var or `local.properties` | Signs access + refresh tokens |
| Server port | `PORT` env var (default 8080) | Ktor embedded server port |
| Firebase credentials | See §23.1 | Admin SDK service account |
| Scheduler interval | Hardcoded 1 hour in `NotificationScheduler` (to be implemented) | Fee/calendar reminder check frequency |

### 23.4 Build Configuration

**Android client**:
- `composeApp/build.gradle.kts` — Firebase dependencies via `google-services` plugin
- `AndroidManifest.xml` — `POST_NOTIFICATIONS` permission, `FirebaseMessagingService` registered
- `composeApp/src/androidMain/res/raw/` — notification sound files (to be added)

**iOS client**:
- `iosApp/iosApp/Info.plist` — `FirebaseAppDelegateProxyEnabled`, `UNUserNotificationCenter` delegate
- `iosApp/iosApp/` — `GoogleService-Info.plist`, sound files (.wav)

**Server**:
- `server/build.gradle.kts` — `firebase-admin` dependency
- `gradle/libs.versions.toml` — version catalog for Firebase Admin SDK

### 23.5 Feature Flags

No feature flag system currently exists. Recommended flags for phased rollout:

| Flag | Default | Description |
|---|---|---|
| `NOTIFICATION_PUSH_ENABLED` | `true` | Master switch for FCM push dispatch. When `false`, `Notify.toUsers()` skips the push bridge. |
| `NOTIFICATION_SCHEDULED_JOBS_ENABLED` | `false` | Enable/disable fee + calendar reminder jobs |
| `NOTIFICATION_RATE_LIMIT_ENABLED` | `false` | Enable/disable per-user rate limiting |
| `NOTIFICATION_PREFERENCES_ENABLED` | `false` | Enable/disable notification preferences screen + API |
| `NOTIFICATION_GROUPING_ENABLED` | `false` | Enable/disable notification grouping/stacking |
| `NOTIFICATION_CUSTOM_VIEWS_ENABLED` | `false` | Enable/disable custom notification layouts |

**Implementation**: Read from env vars at server startup. Cache in a singleton. Check in `Notify.toUsers()` and `NotificationService.send()`.

---

## 24. Operational Requirements

### 24.1 Monitoring

| Metric | Source | Alert Threshold |
|---|---|---|
| Push delivery success rate | `NotificationService.send()` response tallies | < 90% over 1 hour → investigate |
| Push dispatch latency | Time from `send()` entry to batch response | > 10 seconds per batch → investigate |
| In-app notification creation rate | `Notify.toUsers()` call count | Sudden spike (> 10x baseline) → possible bug or abuse |
| Invalid token deactivation rate | `handleFailure()` calls | High rate (> 20% of sends) → stale token cleanup needed |
| Token registration failures | `DeviceTokenRegistrar` API error count | > 5% failure rate → check API health |
| Scheduled job execution | `NotificationScheduler` tick count | No tick in 2 hours → scheduler crashed |
| FCM quota usage | Firebase console | > 80% of quota → request increase or optimize |

### 24.2 Logging

See §6.12 for the full logging strategy. Key points:
- `println()` currently used (no structured logging framework)
- **Recommended**: Migrate to `slf4j` or `kotlin-logging` for structured logs
- Log levels: INFO (init, token lifecycle), WARN (failures, degradation), DEBUG (dispatch details)
- **No PII**: Never log full tokens, notification bodies, or phone numbers. Always mask.

### 24.3 Metrics

| Metric | Type | Description |
|---|---|---|
| `notifications_created_total` | Counter | Total in-app notifications created (`Notify.toUsers()` calls) |
| `notifications_pushed_total` | Counter | Total FCM pushes sent (successful) |
| `notifications_push_failed_total` | Counter | Total FCM pushes failed |
| `notifications_tokens_active` | Gauge | Current count of active device tokens |
| `notifications_tokens_deactivated_total` | Counter | Total tokens deactivated (invalid) |
| `notifications_unread_per_user` | Histogram | Distribution of unread counts per user |
| `notifications_dispatch_duration_seconds` | Histogram | Time spent in `NotificationService.send()` |
| `notifications_scheduled_job_duration_seconds` | Histogram | Time spent in scheduled job execution |

**Implementation**: Expose via `/metrics` endpoint (Prometheus format) or log as structured JSON.

### 24.4 Alerting

| Alert | Condition | Severity |
|---|---|---|
| Firebase init failure | `FirebaseAdminInitializer.app() == null` on startup | Critical — push disabled |
| Push success rate < 50% | Over 1 hour window | High — investigate FCM credentials or token health |
| Scheduled job not running | No execution in 2 hours | Medium — check server health |
| Token registration API errors > 10% | Over 1 hour | Medium — check API endpoint health |
| Notification creation rate spike | > 10x baseline in 1 hour | Low — possible bug or abuse |

### 24.5 Debugging Support

| Tool | Description |
|---|---|
| `POST /api/admin/notifications/send` | Manual push testing — send to specific user IDs from Postman |
| FCM Diagnostics | Firebase console → Cloud Messaging → Diagnostics. Send test message to a specific token. |
| `device_tokens` table inspection | Query `SELECT * FROM device_tokens WHERE user_id = ?` to verify registration |
| `notifications` table inspection | Query `SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 10` to verify in-app delivery |
| Log filtering | `grep "NOTIFY_DISPATCH" server.log` — see all push dispatch logs |
| Log filtering | `grep "FIREBASE_INIT" server.log` — see Firebase initialization status |
| Log filtering | `grep "DEVICE_TOKEN" server.log` — see token registration events |
| `adb shell dumpsys notification` | Inspect active notifications on Android device |
| `adb shell am broadcast -a com.google.firebase.MESSAGING_EVENT` | Trigger FCM message processing for testing |

---

## Appendix A: File Index

### Server

| File | Role |
|---|---|
| `server/.../feature/notifications/Notify.kt` | In-app notification write path (needs push bridge) |
| `server/.../feature/notifications/NotifyRecipients.kt` | Recipient resolvers (parents/teachers/admins) |
| `server/.../feature/notifications/NotificationsRouting.kt` | Inbox API (GET, PATCH, POST read state) |
| `server/.../feature/notification/service/NotificationService.kt` | FCM push dispatch |
| `server/.../feature/notification/api/NotificationRouting.kt` | Device token + admin broadcast API |
| `server/.../feature/notification/repository/DeviceTokenRepository.kt` | Device token CRUD |
| `server/.../feature/notification/firebase/FirebaseAdminInitializer.kt` | Firebase Admin SDK init |
| `server/.../feature/notification/dto/SendNotificationDtos.kt` | Push request/response DTOs |
| `server/.../db/Tables.kt` | `NotificationsTable`, `DeviceTokensTable` |

### Shared (common + platform)

| File | Role |
|---|---|
| `shared/.../feature/notification/domain/service/NotificationService.kt` | Common interface |
| `shared/.../feature/notification/data/remote/NotificationApi.kt` | Ktor client for token registration |
| `shared/.../feature/notification/domain/model/RegisterDeviceTokenRequest.kt` | Request DTO |
| `shared/.../notification/DeviceTokenRegistrar.kt` | Android token sync (fetch → compare → register) |
| `shared/.../notification/AndroidNotificationService.kt` | Android permission flow |
| `shared/.../feature/parent/presentation/NotificationsViewModel.kt` | Inbox VM |
| `shared/.../feature/parent/data/remote/ParentApi.kt` | Inbox API client |
| `shared/.../presentation/PermissionViewModel.kt` | Notification permission VM |
| `shared/.../presentation/MainViewModel.kt` | Triggers token sync on auth |
| `shared/.../core/prefs/PreferenceRepository.kt` | FCM token cache + declined flag |

### Compose App (Android)

| File | Role |
|---|---|
| `composeApp/.../notification/VidyaPrayagFirebaseMessagingService.kt` | FCM receiver |
| `composeApp/.../notification/NotificationManagerHelper.kt` | System tray display + channels |
| `composeApp/.../MainActivity.kt` | Needs deep-link extraction |
| `composeApp/.../EnRollPlusApp.kt` | Channel creation at startup |
| `composeApp/src/androidMain/AndroidManifest.xml` | Permissions + service registration |
| `composeApp/.../ui/v2/screens/notifications/NotificationsScreenV2.kt` | Inbox UI |
| `composeApp/.../ui/v2/navigation/NavGraphV2.kt` | Needs deep-link routing |

### Database

| File | Role |
|---|---|
| `database/migrations/setup_notification_foundation.sql` | `device_tokens` table migration |
| `docs/db/` | Additional migrations directory |

---

## Appendix B: FCM Data Payload Key Contract

The server and client must agree on these keys. The server's `NotificationService.buildDataPayload()` sets them; the client's `VidyaPrayagFirebaseMessagingService` reads them.

| Key | Set By | Read By | Example |
|---|---|---|---|
| `title` | `NotificationService.buildDataPayload()` | `VidyaPrayagFirebaseMessagingService.KEY_TITLE` | `"Attendance update"` |
| `body` | `NotificationService.buildDataPayload()` | `VidyaPrayagFirebaseMessagingService.KEY_BODY` | `"Your child was marked absent..."` |
| `type` | `Notify` bridge (from `category`) | `VidyaPrayagFirebaseMessagingService.KEY_TYPE` | `"attendance"` |
| `deepLink` | `NotificationService.buildDataPayload()` | `VidyaPrayagFirebaseMessagingService.KEY_DEEP_LINK` | `"parent/academics/attendance"` |
| `entityId` | Caller's `data` map | `VidyaPrayagFirebaseMessagingService.KEY_ENTITY_ID` | `"STU-001"` |
| `schoolId` | Caller's `data` map | `VidyaPrayagFirebaseMessagingService.KEY_SCHOOL_ID` | `"uuid-string"` |

> **Note**: The `type` key carries the notification category (`attendance`, `marks`, `homework`, `announcement`, `leave`, `link_request`, `fees`, `message`, `calendar`, `auth`, `general`). The client uses this to select the correct notification channel, icon, and sound.
