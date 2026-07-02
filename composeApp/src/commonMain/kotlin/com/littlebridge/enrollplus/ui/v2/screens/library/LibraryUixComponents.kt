package com.littlebridge.enrollplus.ui.v2.screens.library

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * UIX-003: Color-coded badge showing days remaining until due date.
 * - Green: > 3 days remaining
 * - Yellow: 1-3 days remaining
 * - Red + pulse: overdue (0 or negative)
 */
@Composable
fun DueDateBadge(
    dueDate: String,
    modifier: Modifier = Modifier,
) {
    val daysRemaining = parseDaysRemaining(dueDate)
    val tone = when {
        daysRemaining < 0 -> VBadgeTone.Danger
        daysRemaining <= 3 -> VBadgeTone.Warning
        else -> VBadgeTone.Success
    }
    val text = when {
        daysRemaining < 0 -> "${-daysRemaining}d overdue"
        daysRemaining == 0 -> "Due today"
        else -> "${daysRemaining}d left"
    }

    if (daysRemaining < 0) {
        val transition = rememberInfiniteTransition(label = "overdue_pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "pulse_alpha",
        )
        VBadge(text = text, tone = tone, modifier = modifier.alpha(alpha))
    } else {
        VBadge(text = text, tone = tone, modifier = modifier)
    }
}

/**
 * UIX-004: Progress bar showing fine accumulation vs replacement cost cap.
 */
@Composable
fun FineMeter(
    currentFine: Double,
    replacementCost: Double?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Fine: ₹${"%.2f".format(currentFine)}", style = VTheme.type.caption.colored(c.warningInk))
            if (replacementCost != null && replacementCost > 0) {
                Text("Cap: ₹${"%.2f".format(replacementCost)}", style = VTheme.type.caption.colored(c.ink3))
            } else {
                Text("No cap", style = VTheme.type.caption.colored(c.ink3))
            }
        }
        Spacer(Modifier.height(4.dp))
        if (replacementCost != null && replacementCost > 0) {
            val progress = (currentFine / replacementCost).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) c.danger else c.warning,
                trackColor = c.cream,
            )
            if (progress >= 1f) {
                Text("Fine capped at replacement cost", style = VTheme.type.caption.colored(c.dangerInk))
            }
        } else {
            Text("₹${"%.2f".format(currentFine)} (no cap)", style = VTheme.type.body.colored(c.warningInk).copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * UIX-017: Skeleton loading placeholder for book cards.
 */
@Composable
fun BookCardSkeleton(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(c.cream))
        Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(c.cream))
        Box(Modifier.fillMaxWidth(0.3f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(c.cream))
    }
}

internal fun parseDaysRemaining(dueDate: String): Int {
    return runCatching {
        val due = java.time.LocalDate.parse(dueDate.substringBefore("T"))
        val today = java.time.LocalDate.now()
        java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
    }.getOrDefault(0)
}

/**
 * R21: Book cover with generated initials fallback when no cover_url.
 */
@Composable
fun BookCover(
    title: String,
    author: String?,
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    if (!coverUrl.isNullOrBlank()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = "Cover for $title",
            modifier = modifier.clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        val initials = title.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
        val bgColor = when (title.firstOrNull()?.code?.rem(5)) {
            0 -> c.accent
            1 -> c.successInk
            2 -> c.warningInk
            3 -> c.navy
            else -> c.accentDeep
        }
        Box(
            modifier = modifier.clip(RoundedCornerShape(6.dp)).background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = VTheme.type.h2.colored(c.background),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * R20: Availability badge with 3-tier color coding.
 * - Green: available_copies > 0
 * - Yellow: available_copies = 0 but reservations < 3
 * - Red: available_copies = 0 and reservations >= 3
 */
@Composable
fun AvailabilityBadge(
    availableCopies: Int,
    totalCopies: Int,
    reservationCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val tone = when {
        availableCopies > 0 -> VBadgeTone.Success
        reservationCount < 3 -> VBadgeTone.Warning
        else -> VBadgeTone.Danger
    }
    val text = "$availableCopies/$totalCopies available"
    VBadge(text = text, tone = tone, modifier = modifier)
}

/**
 * Tag chips for book cards.
 */
@Composable
fun TagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.take(3).forEach { tag ->
            VBadge(text = "#$tag", tone = VBadgeTone.Neutral)
        }
        if (tags.size > 3) {
            VBadge(text = "+${tags.size - 3}", tone = VBadgeTone.Neutral)
        }
    }
}

/**
 * UIX-016: Book spine — a vertical, book-spine-styled visual element for
 * shelf/carousel displays. Renders the title rotated 90° on a colored band.
 */
@Composable
fun BookSpine(
    title: String,
    color: Color = VTheme.colors.accent,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = VTheme.type.caption.colored(c.background),
            fontWeight = FontWeight.Bold,
            maxLines = 3,
        )
    }
}

/**
 * UIX-018: Category chip — a clickable pill with the category color dot and name.
 */
@Composable
fun CategoryChip(
    name: String,
    color: String,
    icon: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val bgColor = if (selected) c.accent.copy(alpha = 0.15f) else c.cream
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(parseHexColor(color)))
        if (icon != null) {
            Text(icon, style = VTheme.type.caption.colored(c.ink3))
        }
        Text(
            name,
            style = VTheme.type.caption.colored(if (selected) c.accentDeep else c.ink),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * UIX-029: Filter sheet — a bottom-sheet-style filter panel for book searches.
 * Exposes category, availability, and sort options.
 */
@Composable
fun FilterSheet(
    categories: List<Pair<String, String>>, // (id, name)
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    availability: String,
    onAvailabilityChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filters", style = VTheme.type.bodyStrong.colored(c.ink))

        Text("Category", style = VTheme.type.caption.colored(c.ink2))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VBadge(
                text = "All",
                tone = if (selectedCategory == null) VBadgeTone.Accent else VBadgeTone.Neutral,
                modifier = Modifier.clickable { onCategoryChange(null) },
            )
            categories.forEach { (id, name) ->
                VBadge(
                    text = name,
                    tone = if (selectedCategory == id) VBadgeTone.Accent else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { onCategoryChange(id) },
                )
            }
        }

        Text("Availability", style = VTheme.type.caption.colored(c.ink2))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "All", "available" to "Available Only").forEach { (key, label) ->
                VBadge(
                    text = label,
                    tone = if (availability == key) VBadgeTone.Accent else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { onAvailabilityChange(key) },
                )
            }
        }

        Text("Sort By", style = VTheme.type.caption.colored(c.ink2))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("newest" to "Newest", "title" to "Title A-Z", "author" to "Author", "popular" to "Popular").forEach { (key, label) ->
                VBadge(
                    text = label,
                    tone = if (sortBy == key) VBadgeTone.Accent else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { onSortByChange(key) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VButton(
                text = "Clear",
                onClick = onClear,
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Sand,
                size = VButtonSize.Sm,
                modifier = Modifier.weight(1f),
            )
            VButton(
                text = "Apply Filters",
                onClick = onApply,
                tone = VButtonTone.Lavender,
                size = VButtonSize.Sm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * UIX-031: Trending carousel — a horizontal LazyRow of trending book cards
 * with rank numbers and issue counts.
 */
@Composable
fun TrendingCarousel(
    books: List<TrendingBookEntry>,
    onClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
    ) {
        items(books, key = { it.bookId }) { book ->
            VCard(modifier = Modifier.clickable { onClick(book.bookId) }) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("#${book.rank}", style = VTheme.type.bodyStrong.colored(c.accent))
                    BookCover(
                        title = book.title,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        modifier = Modifier.size(56.dp, 84.dp),
                    )
                    Text(book.title, style = VTheme.type.caption.colored(c.ink), maxLines = 1)
                    book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2), maxLines = 1) }
                    VBadge(text = "${book.issueCount} issues", tone = VBadgeTone.Accent)
                }
            }
        }
    }
}

/**
 * UIX-033: Announcement banner — a dismissible banner shown at the top
 * of library screens for active announcements.
 */
@Composable
fun AnnouncementBanner(
    title: String,
    message: String,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.accent.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📢", style = VTheme.type.body)
            Column {
                Text(title, style = VTheme.type.bodyStrong.colored(c.accentDeep))
                Text(message, style = VTheme.type.caption.colored(c.ink2), maxLines = 2)
            }
        }
        VBadge(text = "✕", tone = VBadgeTone.Neutral, modifier = Modifier.clickable { onDismiss() })
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Simple hex color parser for category colors. */
fun parseHexColor(hex: String): Color {
    return runCatching {
        val normalized = hex.removePrefix("#")
        val r = normalized.substring(0, 2).toInt(16)
        val g = normalized.substring(2, 4).toInt(16)
        val b = normalized.substring(4, 6).toInt(16)
        Color(r / 255f, g / 255f, b / 255f)
    }.getOrDefault(Color(0xFF6C5CE0))
}

/** Trending book entry for the carousel (with rank). */
data class TrendingBookEntry(
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val issueCount: Int,
    val rank: Int,
)
