# Session-Bleed Investigation & Fix

**Branch:** `backend-by-abuzar_v1.0.2`
**Symptom reported:** After logging out of an Admin/Teacher account and logging in
as a Parent, the app renders the **Admin dashboard** with the Parent's name stuck
in skeleton/loading and no data — the Parent dashboard never appears.

This document records what the code *actually does* (read top-to-bottom, nothing
assumed) versus the original hypothesis, and the surgical fix that was applied.

---

## 1. The original hypothesis was tested and largely DISPROVEN

The brief proposed three "certain" causes. Each was checked against the source:

| # | Hypothesis | Verdict | Evidence |
|---|------------|---------|----------|
| 1 | NavGraph routes post-login to the wrong destination using a stale/cached role | **FALSE** | `NavGraphV2` is fully state-driven. `App.kt` collects `MainViewModel.authState` (a `combine` of `prefs.getUserRole()` + `prefs.getUserToken()`, both reactive `StateFlow`s) and passes `role`/`isAuthenticated` down on every recomposition. `EntryRole.from()` normalises with `.uppercase()` and matches every variant. |
| 2 | The Admin dashboard ViewModel is a Koin `single {}` that survives logout | **FALSE (as stated) — but the *effect* is real via a different mechanism** | Every ViewModel in `Koin.kt` is already `factory {}`, not `single {}`. There are **zero** `single {}` ViewModels. So FIX-1 from the brief (change `single→viewModel`) would have been a no-op. |
| 3 | Parent name in skeleton because Admin VM reloads against a parent JWT | **TRUE (symptom)** — but the cause is the ViewModel *cache*, not Koin scope | A surviving `SchoolDashboardViewModel`/`MessagesViewModel` re-fetches admin endpoints with the parent's token and silently stalls in `Loading`. |

The persistence layer, logout, server role handling, and parent auth flow were all
found to be **already correct** (they had been hardened by prior audits:
RA-S01, RA-S03, RA-S05, RA-29, RA-69, RA-41, RA-53, RA-54).

---

## 2. The REAL root cause — a single app-wide `ViewModelStore`

`NavGraphV2` is **not** a `NavHost`. There is no `NavController`, no
`composable()` destinations, no `NavBackStackEntry`. Navigation is a hand-rolled
`AnimatedContent` state machine (`UnauthFlow` / `AuthedFlow` / `RolePortal`).

Consequence: there is exactly **one `ViewModelStoreOwner`** for the whole app —
the platform root (the Android `Activity`, or the desktop/web root). Every
`koinViewModel()` call in every portal and screen resolves against
`LocalViewModelStoreOwner.current` and is **cached in that one store, keyed by
ViewModel class, for the entire process lifetime.**

A `factory {}` Koin registration does **not** save you here: `koinViewModel()`
layers AndroidX's `ViewModelStore` on top of Koin. The store is consulted first;
on a cache hit it returns the **same instance** and never calls the Koin factory
again. So:

```
Admin login   → koinViewModel<SchoolDashboardViewModel>()  → factory creates it,
                store caches it (holds school_id, data, screen state)
Logout        → prefs cleared, isAuthenticated=false → UnauthFlow shown
                ── but the root store is NEVER cleared ──
Parent login  → ParentPortal shown; if any admin screen/VM is touched (or the
                cached MessagesViewModel keeps polling), koinViewModel() returns
                the SAME Admin SchoolDashboardViewModel from before, now firing
                admin requests with the parent JWT → 401/empty → stuck skeleton.
```

That is the session bleed. The `factory {}`/`single {}` distinction is irrelevant;
the bug is the **absence of any per-session teardown of the ViewModelStore**.

---

## 3. The fix — a session-scoped `ViewModelStoreOwner` (`App.kt`)

Introduced `SessionScope`, a composable that provides a fresh `ViewModelStore`
keyed on the live session identity (the JWT, unique per login; a sentinel string
when logged out) to everything below it:

```kotlin
val sessionKey = authState.token?.takeIf { isAuthenticated } ?: "unauthenticated"
SessionScope(sessionKey = sessionKey) {
    NavGraphV2(role = authState.role, isAuthenticated = isAuthenticated,
               onLogout = { viewModel.logout() }, ...)
}

@Composable
private fun SessionScope(sessionKey: String, content: @Composable () -> Unit) {
    val store = remember(sessionKey) { ViewModelStore() }
    val owner = remember(sessionKey) { object : ViewModelStoreOwner {
        override val viewModelStore = store
    } }
    DisposableEffect(sessionKey) { onDispose { store.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}
```

Behaviour:

* **Logout / role switch** changes `sessionKey` → `remember(sessionKey)` rebuilds
  the store and `onDispose` calls `store.clear()` on the outgoing store, running
  `onCleared()` on **every** portal/screen ViewModel from the previous session and
  evicting them. The next login builds all VMs from scratch.
* **`MainViewModel` is resolved ABOVE `SessionScope`** on the root owner, so it is
  never torn down and keeps `authState` reactive across logins — no risk of a dead
  StateFlow freezing navigation.
* The server-side revoke + prefs clear inside `AuthRepositoryImpl.logout()` both
  complete **before** `isAuthenticated` flips false (the prefs write is what flips
  it), so the scope teardown can never abort an in-flight logout.

This is the minimal change that actually addresses the architecture in this repo,
rather than the change the brief prescribed (which assumed a `NavHost` + `single{}`
VMs that do not exist here).

---

## 4. What was verified and left UNCHANGED (already correct)

* **Koin scoping** — all ViewModels already `factory {}`. No change needed.
* **Logout** (`AuthRepositoryImpl.logout`) — server revoke → `prefs.clearSession()`
  → `sessionManager.clearAuthCache()` (evicts Ktor bearer cache, RA-S01) →
  `selectedChildHolder.clear()` (RA-S05). Thorough; unchanged.
* **Post-login role** — `saveSession()` persists `response.role` from the JWT and
  writes the token **last**, so `isAuthenticated` only flips after the fresh role
  is already in place. Reactive `authState` then re-routes. Correct ordering.
* **Role-string consistency** — server stores/returns lowercase
  (`parent`/`school_admin`/`teacher`/`super_admin`); client `EntryRole.from()`
  upcases and matches all variants. No mismatch.
* **Auth form reset** — `ParentAuthScreenV2` and `AdminAuthScreenV2` both call
  `viewModel.reset()` in `LaunchedEffect(Unit)` on mount → blank fields after a
  logout → login round-trip.
* **Parent signup** — `POST /signup` forces `role="parent"`, `profile_completed=false`,
  no `school_id`; mints a parent JWT. `NavGraphV2` routes `EntryRole.Parent` straight
  to the portal (never the school-onboarding gate), regardless of `profileCompleted`
  (RA-S04). Child-not-linked parents reach nearby-schools / link-child from Profile.

---

## 5. Definition of Done — mapping

| DoD item | Addressed by |
|----------|--------------|
| Admin logout → Parent login → Parent dashboard (never Admin) | §3 SessionScope teardown |
| Parent name loads, not stuck in skeleton | §3 — stale Admin VM no longer survives |
| Parent data scoped to linked child | §4 — `SelectedChildHolder.clear()` on logout (pre-existing) + fresh VMs |
| Admin↔Teacher↔Admin switches show correct dashboard | §3 — every role switch flips `sessionKey` |
| No VM instance from session A survives into session B | §3 — `store.clear()` on session change |
| Post-logout auth screen blank | §4 — `viewModel.reset()` on mount (pre-existing) |
| Parent signup row role=parent | §4 — server forces `parent` |
| Parent OTP login returns parent JWT, routes to parent portal | §4 — verified |
| Child-not-linked parent sees nearby schools + link child | §4 — RA-S04 routing (pre-existing) |
