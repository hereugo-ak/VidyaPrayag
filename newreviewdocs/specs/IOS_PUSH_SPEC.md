# iOS Push Notifications (APNs) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None (extends existing notification infrastructure)

---

## 1. Feature Overview

Add Apple Push Notification service (APNs) support to the existing FCM-only notification system, enabling push notifications for iOS users. Integrates with the existing `DeviceTokensTable` and `NotificationsTable` infrastructure.

### Goals

- APNs push delivery for iOS devices
- Multi-device support (existing — multiple tokens per user)
- Silent/background push for data-only notifications
- Rich notifications (images, action buttons)
- Notification grouping (thread identifiers)

---

## 2. Current System Assessment

- `feature_audit.csv` L144: "Server has FCM only, no APNs configuration" — 0%
- `DeviceTokensTable` has `platform` field (android | ios | web) — already supports iOS tokens
- `Notify.kt` dispatches via Firebase Admin SDK which **can** send to APNs via FCM's APNs bridge
- Firebase Admin SDK 9.4.3 supports `ApnsConfig` builder
- No APNs key/certificate configured

### Key Insight

Firebase Cloud Messaging (FCM) can send to iOS devices via APNs bridge. The server already uses Firebase Admin SDK. The main work is:
1. Configure APNs key in Firebase project
2. Add APNs-specific message config in `Notify.kt`
3. iOS client registers for remote notifications and sends token to backend

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No APNs key configured in Firebase | iOS push not delivered |
| G2 | No APNs config in FCM message | iOS notifications have no title/body |
| G3 | iOS client doesn't register for push | No token to send to |
| G4 | No rich notification support | No images, no action buttons |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Configure APNs authentication key (.p8) in Firebase project |
| FR-2 | Add `ApnsConfig` to FCM messages in `Notify.kt` (title, body, sound, badge) |
| FR-3 | iOS client requests push permission and registers token via existing `POST /device-tokens` |
| FR-4 | Support silent push (content-available: 1) for background sync triggers |
| FR-5 | Support rich notifications (mutable-content: 1 + notification service extension) |
| FR-6 | Notification grouping via `thread-id` per conversation/thread |
| FR-7 | Badge count on app icon (unread notification count) |

---

## 5. Backend Architecture

### 5.1 Modify Notify.kt

```kotlin
// In Notify.kt — FCM message builder
fun buildMessage(token: String, notification: NotificationRow, platform: String): Message {
    val builder = Message.builder()
        .setToken(token)
        .setNotification(Notification.builder()
            .setTitle(notification.title)
            .setBody(notification.body)
            .build())

    if (platform == "ios") {
        builder.setApnsConfig(ApnsConfig.builder()
            .setAps(Aps.builder()
                .setBadge(badgeCount)          // unread count
                .setSound("default")
                .setThreadId(notification.threadId)  // grouping
                .setMutableContent(true)        // rich notification support
                .build())
            .putHeader("apns-priority", "10")
            .build())
    }

    // Deep link
    builder.putData("deep_link", notification.deepLink ?: "")
    builder.putData("category", notification.category)

    return builder.build()
}
```

### 5.2 Badge Count

```
GET /api/v1/notifications/badge-count
```

Returns unread notification count. iOS client updates app icon badge via `UIApplication.shared.applicationIconBadge`.

---

## 6. Frontend Architecture (iOS)

### 6.1 Push Registration

```swift
// iOSApp.swift
import UIKit
import Firebase

func registerForPushNotifications() {
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
        if granted {
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }
}

func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    let tokenString = deviceToken.map { String(format: "%02x", $0) }.joined()
    // Send to backend: POST /api/v1/device-tokens
    // { "token": tokenString, "platform": "ios" }
}

func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any], fetchCompletionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
    // Handle notification: navigate to deep link, update badge
    completionHandler(.newData)
}
```

### 6.2 Notification Service Extension (Rich Notifications)

For image attachments in notifications:
- Add `NotificationServiceExtension` target to iOS app
- Downloads image from URL in notification payload
- Attaches image to notification content

---

## 7. Configuration

### 7.1 Firebase Console

1. Generate APNs authentication key (.p8) from Apple Developer Portal
2. Upload to Firebase Console → Project Settings → Cloud Messaging → iOS
3. Configure Bundle ID: `com.littlebridge.enrollplus.ios`

### 7.2 Environment Variables

No new env vars — Firebase Admin SDK uses existing `GOOGLE_APPLICATION_CREDENTIALS`.

---

## 8. Testing Strategy

- Send test push to iOS device via backend → device receives notification
- Silent push → app wakes in background
- Rich notification → image displayed
- Notification grouping → same thread grouped
- Badge count → correct after read/unread changes
- Multi-device: two iOS devices → both receive push

---

## 9. Acceptance Criteria

- [ ] iOS device receives push notification when notification is created
- [ ] Notification has correct title, body, and sound
- [ ] Tapping notification opens app and navigates to deep link
- [ ] Badge count reflects unread notification count
- [ ] Silent push triggers background sync
- [ ] Rich notifications display images
- [ ] Notifications grouped by thread

---

## 10. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | Configure APNs key in Firebase Console |
| 2 | 2 days | Modify Notify.kt to add ApnsConfig |
| 3 | 2 days | iOS push registration + token upload |
| 4 | 1 day | Badge count endpoint |
| 5 | 2 days | Notification service extension (rich notifications) |
| 6 | 1 day | Notification grouping (thread-id) |
| 7 | 2 days | Testing on physical iOS devices |

---

## 11. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/notifications/Notify.kt` | Modify | Add ApnsConfig to FCM messages |
| `server/.../feature/notifications/NotificationsRouting.kt` | Modify | Add badge-count endpoint |
| `iosApp/iosApp/iOSApp.swift` | Modify | Push registration, token upload |
| `iosApp/iosApp/Info.plist` | Modify | Add push notification capabilities |
| `iosApp/NotificationServiceExtension/` | New | Rich notification extension |
