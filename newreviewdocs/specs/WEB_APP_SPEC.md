# Web App — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None (benefits from `TABLET_LAYOUT_SPEC.md` for responsive layouts)

---

## 1. Feature Overview

Browser-based web application providing access to all Vidya Prayag portals (Parent, Teacher, School Admin) via the existing KMP wasmJs/JS target. Reuses Compose Multiplatform UI for web rendering, with SEO-optimized landing page and responsive layouts.

### Goals

- All three portals accessible via web browser (Chrome, Firefox, Safari, Edge)
- URL-based routing (deep links, bookmarkable pages)
- Responsive layouts (desktop, tablet, mobile browser)
- SEO-optimized landing page (server-side rendered or static)
- Web-specific features: file download, print, keyboard shortcuts
- PWA support (installable, offline shell)

---

## 2. Current System Assessment

- `feature_audit.csv` L145: "KMP targets wasmJs/js but no web-specific UI" — 40% complete
- `settings.gradle.kts` and `composeApp/build.gradle.kts` already configure wasmJs/js targets
- `website/` directory exists with Next.js project (separate from KMP app)
- No web entry point for the KMP Compose app
- No URL routing for web

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No web entry point for KMP app | Cannot access portals via browser |
| G2 | No URL routing | No deep links, no bookmarkable pages |
| G3 | No web-specific adaptations | File download, print, keyboard not handled |
| G4 | No PWA manifest | Not installable |
| G5 | Compose web performance | wasm bundle size, initial load time |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Web entry point (`composeApp/src/wasmJsMain/`) rendering Compose UI in browser |
| FR-2 | URL-based routing: `/parent/fees`, `/teacher/attendance`, `/admin/dashboard` |
| FR-3 | Browser back/forward navigation works |
| FR-4 | Responsive layouts (reuse `TABLET_LAYOUT_SPEC.md` adaptive components) |
| FR-5 | Web-specific: file download (receipts, report cards), print (report cards) |
| FR-6 | PWA: manifest.json, service worker for offline shell |
| FR-7 | Keyboard shortcuts (e.g., Ctrl+S for save, Esc for back) |
| FR-8 | Favicon and meta tags |
| FR-9 | wasm bundle optimized (code splitting, lazy loading) |
| FR-10 | Auth: JWT stored in localStorage (web) instead of DataStore |

---

## 5. Frontend Architecture

### 5.1 Web Entry Point

```kotlin
// composeApp/src/wasmJsMain/kotlin/Main.kt
fun main() {
    CanvasBasedWindow(
        title = "Vidya Prayag",
        canvasId = "ComposeTarget"
    ) {
        VidyaPrayagTheme {
            WebApp()
        }
    }
}

@Composable
fun WebApp() {
    val router = rememberWebRouter()
    WebNavHost(router) { route ->
        when (route) {
            is WebRoute.Login -> LoginScreen()
            is WebRoute.ParentFees -> ParentFeesScreen()
            is WebRoute.TeacherAttendance -> TeacherAttendanceScreen()
            is WebRoute.AdminDashboard -> AdminDashboardScreen()
            // ... all routes
        }
    }
}
```

### 5.2 URL Router

```kotlin
class WebRouter {
    private val history = Stack<WebRoute>()
    val currentRoute: StateFlow<WebRoute>

    fun navigate(route: WebRoute) {
        history.push(route)
        updateBrowserUrl(route)
    }

    fun back() {
        history.pop()
        updateBrowserUrl(history.peek())
    }

    private fun updateBrowserUrl(route: WebRoute) {
        // window.history.pushState(null, "", route.urlPath)
    }
}

sealed class WebRoute(val urlPath: String) {
    object Login : WebRoute("/login")
    object ParentHome : WebRoute("/parent")
    object ParentFees : WebRoute("/parent/fees")
    object ParentAcademics : WebRoute("/parent/academics")
    object TeacherHome : WebRoute("/teacher")
    object TeacherAttendance : WebRoute("/teacher/attendance")
    object AdminDashboard : WebRoute("/admin/dashboard")
    // ... all routes
}
```

### 5.3 Platform-Specific Adaptations

```kotlin
// shared/.../core/platform/PlatformCapabilities.kt
expect class PlatformCapabilities() {
    val isWeb: Boolean
    val canDownloadFiles: Boolean
    val canPrint: Boolean
    fun downloadFile(url: String, fileName: String)
    fun print()
}

// wasmJsMain
actual class PlatformCapabilities {
    actual val isWeb = true
    actual val canDownloadFiles = true
    actual val canPrint = true
    actual fun downloadFile(url: String, fileName: String) {
        // Create <a> element, set href, download attribute, click
        val a = js("document.createElement('a')")
        a.href = url
        a.download = fileName
        a.click()
    }
    actual fun print() {
        js("window.print()")
    }
}
```

### 5.4 PWA Configuration

```json
// composeApp/src/wasmJsMain/resources/manifest.json
{
  "name": "Vidya Prayag",
  "short_name": "VidyaPrayag",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#0F172A",
  "theme_color": "#2563EB",
  "icons": [
    {"src": "/icon-192.png", "sizes": "192x192", "type": "image/png"},
    {"src": "/icon-512.png", "sizes": "512x512", "type": "image/png"}
  ]
}
```

### 5.5 HTML Template

```html
<!-- composeApp/src/wasmJsMain/resources/index.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vidya Prayag — School Management</title>
    <link rel="manifest" href="/manifest.json">
    <link rel="icon" href="/favicon.ico">
    <meta name="theme-color" content="#2563EB">
    <style>
        html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; }
        #ComposeTarget { width: 100%; height: 100%; }
        .loading { display: flex; justify-content: center; align-items: center; height: 100vh; }
    </style>
</head>
<body>
    <div id="ComposeTarget">
        <div class="loading">Loading Vidya Prayag...</div>
    </div>
    <script src="vidyaprayag.js"></script>
</body>
</html>
```

---

## 6. Performance Considerations

- wasm bundle size: target < 5MB (use Kotlin/JS IR compiler optimizations)
- Initial load: show loading spinner while wasm downloads
- Code splitting: lazy-load less-used screens (admin features loaded on demand)
- Image optimization: WebP format, lazy loading via Coil
- Service worker: cache wasm bundle + static assets for fast subsequent loads
- Compose web performance: avoid heavy recomposition; use `derivedStateOf` for computed values

---

## 7. Testing Strategy

- Browser testing: Chrome, Firefox, Safari, Edge
- Responsive testing: desktop (1920px), laptop (1366px), tablet (768px), mobile (375px)
- URL routing: back/forward, bookmarkable URLs, deep links
- PWA: installable, offline shell works
- File download: receipts, report cards download correctly
- Print: report cards print correctly

---

## 8. Acceptance Criteria

- [ ] Web app loads in browser at `https://app.vidyaprayag.com`
- [ ] All three portals (Parent, Teacher, Admin) accessible via web
- [ ] URL routing works (back/forward, bookmarkable)
- [ ] Responsive layouts adapt to screen size
- [ ] File download works for receipts and report cards
- [ ] PWA installable on desktop
- [ ] wasm bundle < 5MB
- [ ] Initial load < 5 seconds on broadband

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | Web entry point, HTML template, wasmJs build config |
| 2 | 3 days | URL router + browser navigation integration |
| 3 | 2 days | Platform-specific capabilities (download, print) |
| 4 | 2 days | Auth adaptation (localStorage for web) |
| 5 | 3 days | Apply responsive layouts (reuse TABLET_LAYOUT_SPEC.md) |
| 6 | 1 day | PWA manifest + service worker |
| 7 | 2 days | Performance optimization (bundle size, lazy loading) |
| 8 | 2 days | Deploy + CDN configuration |
| 9 | 2 days | Browser testing + responsive testing |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `composeApp/src/wasmJsMain/kotlin/Main.kt` | New | Web entry point |
| `composeApp/src/wasmJsMain/resources/index.html` | New | HTML template |
| `composeApp/src/wasmJsMain/resources/manifest.json` | New | PWA manifest |
| `composeApp/src/wasmJsMain/kotlin/WebRouter.kt` | New | URL routing |
| `shared/.../core/platform/PlatformCapabilities.kt` | New + expect/actual | Platform abstraction |
| `composeApp/build.gradle.kts` | Modify | wasmJs build config, optimizations |
| `shared/.../core/prefs/TokenStorage.kt` | Modify | Web: localStorage, Mobile: DataStore |
