package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem

/**
 * ENROLL+ TEACHER PORTAL — canonical bottom navigation (Loop task P1-T4).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS DELEGATES TO [TeacherDock] (and is NOT a new pill bar)
 * ─────────────────────────────────────────────────────────────────────────────
 * The loop's P1-T4 sketch — a per-tab `PrimaryIndigoSoft` pill, `scale 1.0→1.15`,
 * a `BadgedBox` — describes a *simpler* nav than what the portal already ships.
 * [TeacherDock] is a far more premium, already-built bottom nav: a floating glass
 * dock with a SPRING-animated sliding violet lozenge, active-glyph lift + scale,
 * a single selection haptic, live count badges, and full TalkBack/VoiceOver
 * semantics — and it is a deliberate sibling of `ParentDock` so a parent and a
 * teacher in the same household perceive ONE product (Doc 10 §12 cross-portal
 * parity contract).
 *
 * Replacing it with a default-Material pill bar would (a) REGRESS below the loop's
 * own QUALITY BAR ("must feel more premium than Byju's/Teachmint/ClassDojo",
 * "anything that looks like a default Material3 component is a failure"), and
 * (b) break parent↔teacher parity — the spirit of the iteration IMPORTANT NOTE
 * ("stick with the theme pattern of the whole Parents + Teacher portal").
 *
 * So P1-T4 is satisfied BY INTENT: this file is the loop's canonical nav ENTRY
 * POINT — a clean `EnrollBottomNav(items, selectedId, onSelect)` API plus the
 * loop's tab-id vocabulary as [EnrollTab] constants — that RENDERS through the
 * superior [TeacherDock]. The loop's required behaviours all map onto dock
 * features that already exist and exceed the spec:
 *   • "selected tab: icon + label / unselected: icon only" → the dock's lozenge
 *     seats the label beside the icon for the active tab only (icon-only inactive).
 *   • "icon tinted PrimaryIndigo, pill PrimaryIndigoSoft" → the dock's active ink +
 *     lozenge are the portal violet `accentDeep` (same family the bridge maps
 *     `PrimaryIndigo` onto — no new colour).
 *   • "animate icon scale on selection" → the dock springs scale + lift (richer
 *     than a flat 1.0→1.15 step).
 *   • "Chat tab unread badge via BadgedBox" → any [VNavItem] carries a live
 *     `badge: Int`; the dock renders a real count badge on the icon (this is also
 *     what P5-T3 wires the Chat unread count into).
 *
 * Lives in the teacher package (beside [TeacherDock]) rather than shared
 * `components/` to keep clean layering — components must not depend on screens.
 *
 * The portal's SHIPPED tab IA is Today · Classes · Gradebook · Planner · Profile
 * (Doc 04 §4 — "attendance is an ACTION, not a tab"). The loop's tab set adds a
 * Chat tab; [EnrollTab] declares the loop vocabulary (incl. Chat) so later loop
 * tasks compile against real ids, while the actual rendered tab list is still
 * supplied by the caller — we do not silently rewrite the live IA here.
 */
@Composable
fun EnrollBottomNav(
    items: List<VNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Render through the premium dock — same physics, badges, a11y, and the
    // portal violet active accent (kept identical to ParentDock for parity).
    TeacherDock(
        items = items,
        selected = selectedId,
        onSelect = onSelect,
        modifier = modifier,
    )
}

/**
 * EnrollBottomNav with a LIVE Chat unread badge (Loop task P5-T3).
 *
 * Overlays the reactive `chatUnread` count onto the [EnrollTab.Chat] item just
 * before rendering, so the badge updates whenever `ChatViewModel.unreadCount`
 * changes — without the caller having to rebuild the whole tab list. The dock's
 * [VNavItem.badge] machinery renders the real count (this is the spec's
 * `BadgedBox` requirement, satisfied by the dock's superior count badge).
 *
 * @param chatUnread the live unread total (e.g. `chatVm.unreadCount.collectAsState()`).
 *   0 hides the badge.
 */
@Composable
fun EnrollBottomNav(
    items: List<VNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    chatUnread: Int,
    modifier: Modifier = Modifier,
) {
    val withBadge = items.map { item ->
        if (item.id == EnrollTab.Chat) item.copy(badge = chatUnread) else item
    }
    EnrollBottomNav(
        items = withBadge,
        selectedId = selectedId,
        onSelect = onSelect,
        modifier = modifier,
    )
}

/**
 * The loop's bottom-nav tab vocabulary (P1-T4). Stable string ids so navigation
 * actions and later loop tasks (e.g. P5-T3 Chat unread badge, P7 deep links)
 * reference real symbols instead of magic strings.
 *
 * NOTE: the live teacher IA today is Today · Classes · Gradebook · Planner ·
 * Profile. [Chat] is declared here as the loop's forward-looking 5th destination
 * (Phase 5); wiring it into the rendered tab list is a Phase-5 concern, not P1-T4.
 */
object EnrollTab {
    const val Home = "home"
    const val Gradebook = "gradebook"
    const val Planner = "planner"
    const val Chat = "chat"
    const val Profile = "profile"

    /** Builds a loop-spec tab list (Home · Gradebook · Planner · Chat · Profile). */
    fun loopTabs(
        homeIcon: ImageVector,
        gradebookIcon: ImageVector,
        plannerIcon: ImageVector,
        chatIcon: ImageVector,
        profileIcon: ImageVector,
        chatUnread: Int = 0,
    ): List<VNavItem> = listOf(
        VNavItem(Home, "Home", homeIcon),
        VNavItem(Gradebook, "Gradebook", gradebookIcon),
        VNavItem(Planner, "Planner", plannerIcon),
        VNavItem(Chat, "Chat", chatIcon, badge = chatUnread), // live unread (P5-T3)
        VNavItem(Profile, "Profile", profileIcon),
    )
}
