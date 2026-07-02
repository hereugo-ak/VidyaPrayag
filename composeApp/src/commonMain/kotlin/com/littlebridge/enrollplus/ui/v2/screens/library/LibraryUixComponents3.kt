package com.littlebridge.enrollplus.ui.v2.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.library.domain.model.FeaturedBookDto
import com.littlebridge.enrollplus.feature.library.domain.model.LibraryBookDto
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

// ── UIX-008: Haptics ──────────────────────────────────────────────────────────

@Composable
fun rememberLibraryHaptics(): LibraryHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { LibraryHaptics(haptic) }
}

class LibraryHaptics(
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    fun tap() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun longPress() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

// ── UIX-011: Swipeable tab content ────────────────────────────────────────────

@Composable
fun SwipeableLibraryTabs(
    tabs: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    content: @Composable (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex) { tabs.size }
    val haptics = rememberLibraryHaptics()

    LaunchedEffect(selected) {
        val idx = tabs.indexOf(selected).coerceAtLeast(0)
        if (pagerState.currentPage != idx) pagerState.animateScrollToPage(idx)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < tabs.size) {
            val newTab = tabs[pagerState.currentPage]
            if (newTab != selected) { haptics.tap(); onSelect(newTab) }
        }
    }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        content(page)
    }
}

// ── UIX-012: Nav badge counts ─────────────────────────────────────────────────

fun libraryNavBadges(
    issuedCount: Int = 0,
    overdueCount: Int = 0,
    reservationCount: Int = 0,
    wishlistCount: Int = 0,
    acquisitionCount: Int = 0,
): Map<String, Int> = buildMap {
    if (issuedCount > 0) put("My Books", issuedCount)
    if (overdueCount > 0) put("History", overdueCount)
    if (reservationCount > 0) put("Reservations", reservationCount)
    if (wishlistCount > 0) put("Wishlist", wishlistCount)
    if (acquisitionCount > 0) put("Requests", acquisitionCount)
}

@Composable
fun TabBadgeCount(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val c = VTheme.colors
    Box(
        modifier.size(18.dp).clip(CircleShape).background(c.danger),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = VTheme.type.caption.colored(c.background),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
        )
    }
}

// ── UIX-014: Featured book card ───────────────────────────────────────────────

@Composable
fun FeaturedBookCard(
    featured: FeaturedBookDto,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val haptics = rememberLibraryHaptics()
    val typeLabel = if (featured.type == "MONTH") "Book of the Month" else "Book of the Week"

    VCard(modifier.fillMaxWidth().clickable { haptics.tap(); onClick() }, elevated = true) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(c.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(VIcons.Star, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                Text(typeLabel, style = VTheme.type.caption.colored(c.accentDeep), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookCover(featured.book.title, featured.book.author, featured.book.coverUrl, Modifier.size(72.dp, 108.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(featured.book.title, style = VTheme.type.bodyStrong.colored(c.ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    featured.book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2), maxLines = 1) }
                    featured.message?.let { Text(it, style = VTheme.type.caption.colored(c.ink3), maxLines = 2) }
                    AvailabilityBadge(featured.book.availableCopies, featured.book.totalCopies)
                }
            }
        }
    }
}

// ── UIX-016: Voice search ─────────────────────────────────────────────────────

@Composable
fun VoiceSearchButton(
    isListening: Boolean,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val haptics = rememberLibraryHaptics()
    Box(
        modifier.size(40.dp).clip(CircleShape)
            .background(if (isListening) c.danger.copy(alpha = 0.15f) else c.cream)
            .clickable { haptics.tap(); onToggleListening() },
        contentAlignment = Alignment.Center,
    ) {
        if (isListening) Text("\u2022", style = VTheme.type.h3.colored(c.dangerInk))
        else Text("\uD83C\uDFA4", style = VTheme.type.body)
    }
}

@Composable
fun VoiceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search books, authors...",
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            VInput(value = query, onValueChange = onQueryChange, placeholder = placeholder, singleLine = true)
        }
        VoiceSearchButton(isListening = isListening, onToggleListening = onToggleListening)
    }
}

// ── UIX-022: Guided quick issue dialog ────────────────────────────────────────

@Composable
fun GuidedQuickIssueDialog(
    visible: Boolean,
    book: LibraryBookDto?,
    borrowerName: String,
    onBorrowerNameChange: (String) -> Unit,
    onIssue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val c = VTheme.colors
    val haptics = rememberLibraryHaptics()
    var step by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Issue", style = VTheme.type.bodyStrong.colored(c.ink))
                VBadge(text = "Step $step/3", tone = VBadgeTone.Accent)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier.size(if (step == i + 1) 24.dp else 8.dp, if (step == i + 1) 4.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (step >= i + 1) c.accent else c.cream)
                        )
                    }
                }
                when (step) {
                    1 -> {
                        Text("Confirm Book", style = VTheme.type.caption.colored(c.ink2))
                        if (book != null) VCard {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BookCover(book.title, book.author, book.coverUrl, Modifier.size(48.dp, 72.dp))
                                Column {
                                    Text(book.title, style = VTheme.type.bodyStrong.colored(c.ink), maxLines = 2)
                                    book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2)) }
                                    AvailabilityBadge(book.availableCopies, book.totalCopies)
                                }
                            }
                        } else Text("No book selected. Search and select a book first.", style = VTheme.type.caption.colored(c.ink3))
                    }
                    2 -> {
                        Text("Borrower Details", style = VTheme.type.caption.colored(c.ink2))
                        VInput(value = borrowerName, onValueChange = onBorrowerNameChange, label = "Borrower name", placeholder = "Enter name", singleLine = true)
                    }
                    3 -> {
                        Text("Review & Confirm", style = VTheme.type.caption.colored(c.ink2))
                        VCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                book?.let {
                                    Text("Book: ${it.title}", style = VTheme.type.body.colored(c.ink))
                                    Text("Author: ${it.author ?: "Unknown"}", style = VTheme.type.caption.colored(c.ink2))
                                }
                                Text("Borrower: $borrowerName", style = VTheme.type.body.colored(c.ink))
                                Text("Due date: 14 days from today", style = VTheme.type.caption.colored(c.ink3))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step < 3) {
                TextButton({
                    if (step == 1 && book == null) return@TextButton
                    if (step == 2 && borrowerName.isBlank()) return@TextButton
                    haptics.confirm(); step++
                }) { Text("Next") }
            } else {
                TextButton({ haptics.confirm(); onIssue(); step = 1 }) { Text("Issue Book") }
            }
        },
        dismissButton = {
            TextButton({ step = 1; onDismiss() }) { Text("Cancel") }
        },
    )
}
