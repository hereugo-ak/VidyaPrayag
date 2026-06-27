# Tablet Layout Adaptivity — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Responsive layout system that adapts UI for tablets (7-12 inch) and large screens. Uses Compose Multiplatform's `WindowSizeClass` to switch between single-pane (phone) and dual-pane (tablet) layouts, with adaptive grids, expanded navigation, and optimized content density.

### Goals

- Dual-pane (master-detail) layouts on tablets for list+detail screens
- Adaptive grid layouts for card-based screens
- Expanded side navigation on tablets (rail → drawer)
- Optimized content density (more items visible without scrolling)
- Smooth transitions at window size breakpoints

---

## 2. Current System Assessment

- `feature_audit.csv` L148: "Some widthIn(max) constraints, no true responsive layouts" — 20% complete
- Compose Multiplatform 1.10.3 supports `WindowSizeClass` API
- All screens currently designed for phone width (single-pane)
- No tablet-specific layouts or breakpoints

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No dual-pane layouts | Tablet users navigate back-and-forth |
| G2 | No adaptive grids | Cards too large on tablet |
| G3 | No expanded navigation | Hamburger menu on tablet is suboptimal |
| G4 | No content density adaptation | Wasted screen space on tablets |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Use `WindowSizeClass` to detect compact/medium/expanded window sizes |
| FR-2 | Dual-pane layout for: Messages, Announcements, Attendance, Homework, Fees, Calendar |
| FR-3 | Adaptive grid: 1 column (compact), 2 columns (medium), 3 columns (expanded) |
| FR-4 | Navigation: bottom bar (compact) → navigation rail (medium) → permanent drawer (expanded) |
| FR-5 | Content density: increase padding/spacing on larger screens |
| FR-6 | Support orientation changes (portrait ↔ landscape) |
| FR-7 | Support split-screen and multi-window on Android |

---

## 5. Breakpoints

| Class | Width | Layout |
|---|---|---|
| Compact | < 600dp | Single-pane, bottom nav |
| Medium | 600-840dp | Dual-pane optional, navigation rail |
| Expanded | > 840dp | Dual-pane, permanent drawer |

---

## 6. Frontend Architecture

### 6.1 AdaptiveLayout Composable

```kotlin
@Composable
fun AdaptiveLayout(
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Single-pane: list fills screen, detail opens as new screen
            list()
        }
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> {
            // Dual-pane: list on left (40%), detail on right (60%)
            Row {
                Box(modifier = Modifier.weight(0.4f)) { list() }
                Box(modifier = Modifier.weight(0.6f)) { detail() }
            }
        }
    }
}
```

### 6.2 AdaptiveGrid Composable

```kotlin
@Composable
fun AdaptiveGrid(
    items: List<T>,
    content: @Composable (T) -> Unit
) {
    val columns = when (currentWindowWidthSizeClass()) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }
    LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
        items(items) { content(it) }
    }
}
```

### 6.3 AdaptiveNavigation

```kotlin
@Composable
fun AdaptiveNavigation(
    items: List<NavItem>,
    content: @Composable () -> Unit
) {
    val widthClass = currentWindowWidthSizeClass()
    when (widthClass) {
        WindowWidthSizeClass.Compact -> {
            Scaffold(bottomBar = { VBottomBar(items) }) { content() }
        }
        WindowWidthSizeClass.Medium -> {
            Scaffold { padding ->
                Row {
                    VNavigationRail(items)
                    Box(modifier = Modifier.padding(padding)) { content() }
                }
            }
        }
        else -> {
            Scaffold { padding ->
                Row {
                    VPermanentDrawer(items)
                    Box(modifier = Modifier.padding(padding)) { content() }
                }
            }
        }
    }
}
```

---

## 7. Testing Strategy

- UI tests with different window sizes (emulated via `WindowAdaptiveInfo`)
- Visual regression: screenshot each screen at compact/medium/expanded
- Orientation change: portrait ↔ landscape layout correct

---

## 8. Acceptance Criteria

- [ ] Dual-pane layout for Messages, Announcements, Attendance, Homework, Fees, Calendar
- [ ] Adaptive grid for card-based screens
- [ ] Navigation adapts: bottom bar → rail → drawer
- [ ] Orientation changes handled smoothly
- [ ] No layout overflow or clipping on tablets

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | AdaptiveLayout + AdaptiveGrid + AdaptiveNavigation composables |
| 2 | 3 days | Apply dual-pane to Messages, Announcements, Attendance |
| 3 | 3 days | Apply dual-pane to Homework, Fees, Calendar |
| 4 | 2 days | Apply adaptive grid to all card screens |
| 5 | 2 days | Navigation adaptation (rail + permanent drawer) |
| 6 | 2 days | Tests (UI + visual regression) |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/components/AdaptiveLayout.kt` | New | Dual-pane composable |
| `composeApp/.../ui/v2/components/AdaptiveGrid.kt` | New | Adaptive grid composable |
| `composeApp/.../ui/v2/components/AdaptiveNavigation.kt` | New | Adaptive navigation |
| `composeApp/.../ui/v2/screens/**/*.kt` | Modify | Apply adaptive layouts to each screen |
| `composeApp/.../ui/v2/components/VNavigationRail.kt` | New | Navigation rail component |
| `composeApp/.../ui/v2/components/VPermanentDrawer.kt` | New | Permanent drawer component |
