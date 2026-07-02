package com.littlebridge.enrollplus.ui.v2.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.library.domain.model.*
import com.littlebridge.enrollplus.feature.library.presentation.StudentLibraryState
import com.littlebridge.enrollplus.feature.library.presentation.StudentLibraryViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VLabel
import com.littlebridge.enrollplus.ui.v2.components.VProgressBar
import com.littlebridge.enrollplus.ui.v2.components.VPullRefresh
import com.littlebridge.enrollplus.ui.v2.screens.VErrorState
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.screens.library.BookCardSkeleton
import com.littlebridge.enrollplus.ui.v2.screens.library.BookShelfView
import com.littlebridge.enrollplus.ui.v2.screens.library.CoachMarkOverlay
import com.littlebridge.enrollplus.ui.v2.screens.library.ExpandableSynopsis
import com.littlebridge.enrollplus.ui.v2.screens.library.GreetingHeader
import com.littlebridge.enrollplus.ui.v2.screens.library.IllustratedEmptyState
import com.littlebridge.enrollplus.ui.v2.screens.library.LibraryViewMode
import com.littlebridge.enrollplus.ui.v2.screens.library.QrShareDialog
import com.littlebridge.enrollplus.ui.v2.screens.library.ReadingStreakTracker
import com.littlebridge.enrollplus.ui.v2.screens.library.ReadingTimeEstimate
import com.littlebridge.enrollplus.ui.v2.screens.library.RecentlyViewedStrip
import com.littlebridge.enrollplus.ui.v2.screens.library.SwipeableIssueCard
import com.littlebridge.enrollplus.ui.v2.screens.library.ViewModeToggle
import com.littlebridge.enrollplus.ui.v2.screens.library.parseDaysRemaining
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

private enum class StudentLibraryTab(val label: String) {
    Browse("Browse"),
    MyBooks("My Books"),
    History("History"),
    Wishlist("Wishlist"),
    Reservations("Reservations"),
    Acquisition("Requests"),
    Profile("Profile"),
    Badges("Badges"),
    Discussions("Discussions"),
}

@Composable
fun StudentLibraryScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StudentLibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var activeTab by remember { mutableStateOf(StudentLibraryTab.Browse) }
    val c = VTheme.colors
    var showCoachMark by remember { mutableStateOf(true) }
    var recentlyViewed by remember { mutableStateOf<List<LibraryBookDto>>(emptyList()) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    var showQrDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadBadges()
        viewModel.loadReadingGoal()
        viewModel.searchBooks(1)
        viewModel.loadTrending()
        viewModel.loadRecommendations()
        viewModel.loadReservations()
        viewModel.loadAcquisitionRequests()
        viewModel.loadAnnouncements()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.background),
    ) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        com.littlebridge.enrollplus.ui.v2.components.VBackHeader(title = "Library", onBack = onBack)

        if (state.isOffline) {
            Row(
                modifier = Modifier.fillMaxWidth().background(c.warning.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("⚠️", style = VTheme.type.caption)
                Text(
                    if (state.isStaleData) "Offline — showing cached data" else "Offline — check your connection",
                    style = VTheme.type.caption.colored(c.warningInk),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StudentLibraryTab.entries.forEach { tab ->
                VBadge(
                    text = tab.label,
                    tone = if (activeTab == tab) VBadgeTone.Accent else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { activeTab = tab },
                )
            }
        }

        when (activeTab) {
            StudentLibraryTab.Browse -> BrowseTab(
                state, viewModel, viewMode, { viewMode = it },
                recentlyViewed,
                { book -> recentlyViewed = (listOf(book) + recentlyViewed.filter { it.id != book.id }).take(10) },
                { recentlyViewed = emptyList() },
                { id, title -> showQrDialog = id to title },
            )
            StudentLibraryTab.MyBooks -> MyBooksTab(state, viewModel)
            StudentLibraryTab.History -> HistoryTab(state, viewModel)
            StudentLibraryTab.Wishlist -> WishlistTab(state, viewModel)
            StudentLibraryTab.Reservations -> ReservationsTab(state, viewModel)
            StudentLibraryTab.Acquisition -> AcquisitionRequestsTab(state, viewModel)
            StudentLibraryTab.Profile -> ProfileTab(state, viewModel)
            StudentLibraryTab.Badges -> BadgesTab(state, viewModel)
            StudentLibraryTab.Discussions -> DiscussionsTab(state, viewModel)
        }
    }

    if (state.actionMessage != null) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            VBadge(text = state.actionMessage!!, tone = VBadgeTone.Accent)
        }
        LaunchedEffect(state.actionMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearActionMessage()
        }
    }

    if (showCoachMark && activeTab == StudentLibraryTab.Browse) {
        CoachMarkOverlay(
            targetText = "Welcome to Library!",
            message = "Search for any book by title, author, or ISBN. Use filters to narrow down results.",
            onDismiss = { showCoachMark = false },
        )
    }

    showQrDialog?.let { (bookId, title) ->
        QrShareDialog(bookId = bookId, bookTitle = title, onDismiss = { showQrDialog = null })
    }
    }
}

@Composable
private fun BrowseTab(
    state: StudentLibraryState,
    viewModel: StudentLibraryViewModel,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    recentlyViewed: List<LibraryBookDto>,
    onBookViewed: (LibraryBookDto) -> Unit,
    onClearRecent: () -> Unit,
    onShareQr: (String, String) -> Unit,
) {
    val c = VTheme.colors
    var dismissedAnnouncements by remember { mutableStateOf(setOf<String>()) }

    VPullRefresh(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.searchBooks(1) },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Greeting header (UIX-016)
        item {
            GreetingHeader(
                userName = state.profile?.let { "Reader" } ?: "Reader",
                overdueCount = state.issuedBooks.count { parseDaysRemaining(it.dueDate) < 0 },
                dueTomorrowCount = state.issuedBooks.count { parseDaysRemaining(it.dueDate) in 0..1 },
                reservationReadyCount = state.reservations.count { it.status == "notified" },
            )
        }

        // Recently viewed strip (UIX-014)
        if (recentlyViewed.isNotEmpty()) {
            item {
                RecentlyViewedStrip(
                    books = recentlyViewed,
                    onBookClick = { /* navigate to detail */ },
                    onClear = onClearRecent,
                )
            }
        }

        // Announcements banner
        val activeAnnouncements = state.announcements.filter { it.id !in dismissedAnnouncements }
        if (activeAnnouncements.isNotEmpty()) {
            items(activeAnnouncements, key = { it.id }) { ann ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(ann.title, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(ann.message, style = VTheme.type.caption.colored(c.ink2), maxLines = 2)
                        }
                        VBadge(
                            text = "×",
                            tone = VBadgeTone.Neutral,
                            modifier = Modifier.clickable { dismissedAnnouncements = dismissedAnnouncements + ann.id },
                        )
                    }
                }
            }
        }

        // Search bar
        item {
            VInput(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = "Search books",
                modifier = Modifier.fillMaxWidth(),
            )
            VButton(
                text = "Search",
                onClick = { viewModel.searchBooks(1) },
                full = true,
                tone = VButtonTone.Lavender,
                size = VButtonSize.Md,
            )
        }

        // Trending carousel
        if (state.trending.isNotEmpty()) {
            item {
                Text("Trending Now", style = VTheme.type.bodyStrong.colored(c.ink))
                Spacer(Modifier.height(4.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.trending, key = { it.bookId }) { book ->
                        VCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                com.littlebridge.enrollplus.ui.v2.screens.library.BookCover(
                                    title = book.title,
                                    author = book.author,
                                    coverUrl = book.coverUrl,
                                    modifier = Modifier.size(48.dp, 72.dp),
                                )
                                Text(book.title, style = VTheme.type.bodyStrong.colored(c.ink), maxLines = 1)
                                book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2), maxLines = 1) }
                                VBadge(text = "${book.issueCount} issues", tone = VBadgeTone.Accent)
                            }
                        }
                    }
                }
            }
        }

        // Recommendations
        if (state.recommendations.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Recommended For You", style = VTheme.type.bodyStrong.colored(c.ink))
                Spacer(Modifier.height(4.dp))
            }
            items(state.recommendations.take(5), key = { it.bookId }) { rec ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        com.littlebridge.enrollplus.ui.v2.screens.library.BookCover(
                            title = rec.title,
                            author = rec.author,
                            coverUrl = rec.coverUrl,
                            modifier = Modifier.size(48.dp, 72.dp),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(rec.title, style = VTheme.type.bodyStrong.colored(c.ink))
                            rec.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2)) }
                            rec.category?.let { VBadge(text = it, tone = VBadgeTone.Neutral) }
                            rec.reason?.let { Text("Why: $it", style = VTheme.type.caption.colored(c.ink3), maxLines = 2) }
                        }
                    }
                }
            }
        }

        // Loading skeletons
        if (state.isLoading && state.books.isEmpty()) {
            items(3) { BookCardSkeleton() }
            return@LazyColumn
        }

        // Error state
        if (state.error != null && state.books.isEmpty()) {
            item { VErrorState(message = state.error ?: "", onRetry = { viewModel.searchBooks(1) }) }
            return@LazyColumn
        }

        // Search results
        if (state.books.isEmpty()) {
            item { IllustratedEmptyState(title = "No books found", body = "Try a different search query.", icon = VIcons.Search) }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${state.booksTotal} books", style = VTheme.type.caption.colored(c.ink3))
                    ViewModeToggle(viewMode = viewMode, onModeChange = onViewModeChange)
                }
            }
            if (viewMode == LibraryViewMode.SHELF) {
                item {
                    BookShelfView(
                        books = state.books,
                        onBookClick = { id -> onBookViewed(state.books.first { it.id == id }) },
                    )
                }
            } else {
                items(state.books, key = { it.id }) { book ->
                    BookCard(book, state.isActionLoading, viewModel, onShareQr = onShareQr, onView = onBookViewed)
                }
            }
            // Pagination
            if (state.books.size < state.booksTotal) {
                item {
                    VButton(
                        text = "Load More (${state.booksTotal - state.books.size} remaining)",
                        onClick = { viewModel.searchBooks(state.booksPage + 1) },
                        full = true,
                        variant = VButtonVariant.Secondary,
                        tone = VButtonTone.Sky,
                        size = VButtonSize.Sm,
                        loading = state.isLoading,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun BookCard(
    book: LibraryBookDto,
    isActionLoading: Boolean,
    viewModel: StudentLibraryViewModel,
    onShareQr: (String, String) -> Unit = { _, _ -> },
    onView: (LibraryBookDto) -> Unit = {},
) {
    val c = VTheme.colors
    VCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                com.littlebridge.enrollplus.ui.v2.screens.library.BookCover(
                    title = book.title,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    modifier = Modifier.size(64.dp, 96.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(book.title, style = VTheme.type.bodyStrong.colored(c.ink))
                    book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2)) }
                    if (book.seriesName != null) {
                        Text("${book.seriesName} #${book.seriesNumber ?: 1}", style = VTheme.type.caption.colored(c.ink3))
                    }
                    ReadingTimeEstimate(pageCount = book.pageCount)
                }
            }
            // Expandable synopsis (UIX-018)
            book.synopsis?.let { ExpandableSynopsis(synopsis = it) }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.littlebridge.enrollplus.ui.v2.screens.library.AvailabilityBadge(
                    availableCopies = book.availableCopies,
                    totalCopies = book.totalCopies,
                )
                book.category?.let { VBadge(text = it, tone = VBadgeTone.Neutral) }
                if (book.language != "en") VBadge(text = book.language, tone = VBadgeTone.Neutral)
            }
            com.littlebridge.enrollplus.ui.v2.screens.library.TagChips(tags = book.tags)
            if (book.availableCopies == 0) {
                VButton(
                    text = "Reserve",
                    onClick = { viewModel.reserveBook(book.id) },
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Sm,
                    loading = isActionLoading,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VButton(
                    text = "+ Wishlist",
                    onClick = { viewModel.addToWishlist(book.id) },
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Sky,
                    size = VButtonSize.Sm,
                    loading = isActionLoading,
                )
                VButton(
                    text = "Share",
                    onClick = { onShareQr(book.id, book.title) },
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Sand,
                    size = VButtonSize.Sm,
                    leading = { Icon(VIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

@Composable
private fun ProfileTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors
    val p = state.profile

    if (state.isLoading && p == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(36.dp))
        }
        return
    }

    if (state.error != null && p == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            VErrorState(message = state.error ?: "", onRetry = { viewModel.loadProfile() })
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("My Library Profile", style = VTheme.type.h2.colored(c.ink))

        VCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("Books Read", p?.totalBooksRead?.toString() ?: "0")
                ProfileStat("Currently Issued", p?.currentlyIssued?.toString() ?: "0")
                ProfileStat("Overdue", p?.overdueCount?.toString() ?: "0")
                ProfileStat("Outstanding Fine", "₹${"%.2f".format(p?.outstandingFine ?: 0.0)}")
                ProfileStat("Current Streak", "${p?.currentStreak ?: 0} days")
                ProfileStat("Longest Streak", "${p?.longestStreak ?: 0} days")
            }
        }

        // Reading Streak Tracker (UIX-025)
        ReadingStreakTracker(
            currentStreak = p?.currentStreak ?: 0,
            longestStreak = p?.longestStreak ?: 0,
        )

        val goal = state.readingGoal
        if (goal != null) {
            Text("Reading Goal", style = VTheme.type.h2.colored(c.ink))
            VCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        VLabel("${goal.period.replaceFirstChar { it.uppercase() }} ${goal.targetYear}")
                        VBadge(
                            text = "${goal.booksRead}/${goal.goalCount}",
                            tone = if (goal.isAchieved) VBadgeTone.Success else VBadgeTone.Accent,
                        )
                    }
                    VProgressBar(
                        value = goal.progressPercentage.toFloat(),
                        tone = if (goal.isAchieved) VBadgeTone.Success else VBadgeTone.Accent,
                    )
                    if (goal.isAchieved) {
                        Text("Goal achieved! 🎉", style = VTheme.type.caption.colored(c.successInk))
                    }
                }
            }
        }

        // Set / Update Reading Goal
        Text("Set Reading Goal", style = VTheme.type.h2.colored(c.ink))
        var goalCount by remember { mutableStateOf("5") }
        var period by remember { mutableStateOf("monthly") }
        var targetYear by remember { mutableStateOf(java.time.LocalDate.now().year.toString()) }
        VCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VInput(value = goalCount, onValueChange = { goalCount = it }, label = "Goal (number of books)", modifier = Modifier.fillMaxWidth())
                Text("Period", style = VTheme.type.caption.colored(c.ink2))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("monthly" to "Monthly", "quarterly" to "Quarterly", "yearly" to "Yearly").forEach { (key, label) ->
                        VBadge(
                            text = label,
                            tone = if (period == key) VBadgeTone.Accent else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { period = key },
                        )
                    }
                }
                VInput(value = targetYear, onValueChange = { targetYear = it }, label = "Target Year", modifier = Modifier.fillMaxWidth())
                VButton(
                    text = "Set Goal",
                    onClick = {
                        viewModel.setReadingGoal(
                            goalCount.toIntOrNull() ?: 5,
                            period,
                            targetYear.toIntOrNull() ?: java.time.LocalDate.now().year,
                        )
                    },
                    full = true,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Md,
                    loading = state.isActionLoading,
                )
            }
        }
    }
}

@Composable
private fun BadgesTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors

    if (state.isLoading && state.badges.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(36.dp))
        }
        return
    }

    if (state.badges.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            IllustratedEmptyState(title = "No badges yet", body = "Read more books to earn badges!", icon = VIcons.Star)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.badges, key = { it.badgeType }) { badge ->
            VCard {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(badge.badgeName, style = VTheme.type.bodyStrong.colored(c.ink))
                        badge.earnedAt?.let {
                            Text("Earned: $it", style = VTheme.type.caption.colored(c.ink2))
                        }
                    }
                    VBadge(
                        text = if (badge.isEarned) "Earned" else "Locked",
                        tone = if (badge.isEarned) VBadgeTone.Success else VBadgeTone.Neutral,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        VLabel(label)
        Text(value, style = VTheme.type.body.colored(c.ink).copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun MyBooksTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors

    LaunchedEffect(Unit) { viewModel.loadIssuedBooks() }

    VPullRefresh(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.loadIssuedBooks() },
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Books", style = VTheme.type.h2.colored(c.ink))

        if (state.error != null && state.issuedBooks.isEmpty()) {
            VErrorState(message = state.error ?: "", onRetry = { viewModel.loadIssuedBooks() })
            return@Column
        }

        if (state.isLoading && state.issuedBooks.isEmpty()) {
            repeat(2) { BookCardSkeleton() }
            return@Column
        }

        if (state.issuedBooks.isEmpty()) {
            IllustratedEmptyState(title = "No books issued", body = "Browse the library and issue a book to get started.", icon = VIcons.BookOpen)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.issuedBooks, key = { it.id }) { issue ->
                SwipeableIssueCard(
                    canRenew = issue.renewalCount < 2,
                    onReturn = { viewModel.renewBook(issue.id) },
                    onRenew = { viewModel.renewBook(issue.id) },
                ) {
                    VCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(issue.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                            com.littlebridge.enrollplus.ui.v2.screens.library.DueDateBadge(dueDate = issue.dueDate)
                            VBadge(
                                text = "Renewals: ${issue.renewalCount}/2",
                                tone = if (issue.renewalCount >= 2) VBadgeTone.Warning else VBadgeTone.Neutral,
                            )
                            if (issue.fineAmount > 0 && issue.fineStatus == "pending") {
                                VBadge(
                                    text = "Fine: ₹${"%.2f".format(issue.fineAmount)}",
                                    tone = VBadgeTone.Warning,
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                VButton(
                                    text = "Renew",
                                    onClick = { viewModel.renewBook(issue.id) },
                                    variant = VButtonVariant.Secondary,
                                    tone = VButtonTone.Lavender,
                                    size = VButtonSize.Sm,
                                    loading = state.isActionLoading,
                                    enabled = issue.renewalCount < 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HistoryTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reading History", style = VTheme.type.h2.colored(c.ink))

        if (state.error != null && state.history.isEmpty()) {
            VErrorState(message = state.error ?: "", onRetry = { viewModel.loadHistory() })
            return@Column
        }

        if (state.isLoading && state.history.isEmpty()) {
            repeat(2) { BookCardSkeleton() }
            return@Column
        }

        if (state.history.isEmpty()) {
            IllustratedEmptyState(title = "No history yet", body = "Your reading history will appear here.", icon = VIcons.Clock)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.history, key = { it.id }) { issue ->
                VCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(issue.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            VBadge(
                                text = issue.status.replaceFirstChar { it.uppercase() },
                                tone = when (issue.status) {
                                    "returned" -> VBadgeTone.Success
                                    "lost" -> VBadgeTone.Danger
                                    else -> VBadgeTone.Neutral
                                },
                            )
                            if (issue.fineAmount > 0) {
                                VBadge(
                                    text = "₹${"%.2f".format(issue.fineAmount)} ${issue.fineStatus}",
                                    tone = when (issue.fineStatus) {
                                        "pending" -> VBadgeTone.Warning
                                        "paid" -> VBadgeTone.Success
                                        "waived" -> VBadgeTone.Neutral
                                        else -> VBadgeTone.Neutral
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors

    LaunchedEffect(Unit) { viewModel.loadWishlist() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Wishlist", style = VTheme.type.h2.colored(c.ink))

        if (state.isLoading && state.wishlist.isEmpty()) {
            repeat(2) { BookCardSkeleton() }
            return@Column
        }

        if (state.wishlist.isEmpty()) {
            if (state.error != null) {
                VErrorState(message = state.error ?: "", onRetry = { viewModel.loadWishlist() })
                return@Column
            }
            IllustratedEmptyState(title = "Wishlist is empty", body = "Add books to your wishlist to read later.", icon = VIcons.Heart)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.wishlist, key = { it.id }) { item ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(item.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                            item.addedAt?.let { Text(it, style = VTheme.type.caption.colored(c.ink2)) }
                        }
                        VButton(
                            text = "Remove",
                            onClick = { viewModel.removeFromWishlist(item.bookId) },
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Rose,
                            size = VButtonSize.Sm,
                            loading = state.isActionLoading,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationsTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors
    var showCancelConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadReservations() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Reservations", style = VTheme.type.h2.colored(c.ink))

        if (state.isLoading && state.reservations.isEmpty()) {
            repeat(2) { BookCardSkeleton() }
            return@Column
        }

        if (state.reservations.isEmpty()) {
            if (state.error != null) {
                VErrorState(message = state.error ?: "", onRetry = { viewModel.loadReservations() })
                return@Column
            }
            IllustratedEmptyState(title = "No reservations", body = "Reserve a book from the Browse tab to see it here.", icon = VIcons.BookOpen)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.reservations, key = { it.id }) { res ->
                VCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(res.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VBadge(
                                text = res.status.replaceFirstChar { it.uppercase() },
                                tone = when (res.status) {
                                    "pending" -> VBadgeTone.Warning
                                    "notified" -> VBadgeTone.Accent
                                    "fulfilled" -> VBadgeTone.Success
                                    "cancelled" -> VBadgeTone.Neutral
                                    else -> VBadgeTone.Neutral
                                },
                            )
                            res.waitlistPosition?.let { VBadge(text = "#$it", tone = VBadgeTone.Neutral) }
                        }
                        Text("Reserved: ${res.createdAt}", style = VTheme.type.caption.colored(c.ink3))

                        if (res.status == "pending" || res.status == "notified") {
                            VButton(
                                text = "Cancel",
                                onClick = { showCancelConfirm = res.id },
                                variant = VButtonVariant.Secondary,
                                tone = VButtonTone.Rose,
                                size = VButtonSize.Sm,
                                loading = state.isActionLoading,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCancelConfirm != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = null },
            title = { Text("Cancel Reservation?") },
            text = { Text("Are you sure you want to cancel this reservation?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelReservation(showCancelConfirm!!)
                    showCancelConfirm = null
                }) { Text("Cancel Reservation") }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirm = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun AcquisitionRequestsTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors

    LaunchedEffect(Unit) { viewModel.loadAcquisitionRequests() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Acquisition Requests", style = VTheme.type.h2.colored(c.ink))

        if (state.acquisitionRequests.isEmpty()) {
            if (state.error != null) {
                VErrorState(message = state.error ?: "", onRetry = { viewModel.loadAcquisitionRequests() })
                return@Column
            }
            IllustratedEmptyState(title = "No requests", body = "Your book acquisition requests will appear here.", icon = VIcons.Plus)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.acquisitionRequests, key = { it.id }) { req ->
                VCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(req.title, style = VTheme.type.bodyStrong.colored(c.ink))
                        req.author?.let { Text("Author: $it", style = VTheme.type.caption.colored(c.ink2)) }
                        req.isbn?.let { Text("ISBN: $it", style = VTheme.type.caption.colored(c.ink2)) }
                        req.reason?.let { Text("Reason: $it", style = VTheme.type.caption.colored(c.ink3)) }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VBadge(
                                text = req.status.replaceFirstChar { it.uppercase() },
                                tone = when (req.status) {
                                    "pending" -> VBadgeTone.Warning
                                    "approved" -> VBadgeTone.Accent
                                    "ordered" -> VBadgeTone.Neutral
                                    "received" -> VBadgeTone.Success
                                    "rejected" -> VBadgeTone.Danger
                                    else -> VBadgeTone.Neutral
                                },
                            )
                            Text(req.createdAt, style = VTheme.type.caption.colored(c.ink3))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionsTab(state: StudentLibraryState, viewModel: StudentLibraryViewModel) {
    val c = VTheme.colors
    var bookId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Book Discussions", style = VTheme.type.h2.colored(c.ink))

        VCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VInput(value = bookId, onValueChange = { bookId = it }, label = "Book ID", modifier = Modifier.fillMaxWidth())
                VButton(
                    text = "Load Discussions",
                    onClick = { if (bookId.isNotBlank()) viewModel.loadDiscussions(bookId) },
                    full = true,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Sm,
                )
            }
        }

        if (state.discussions.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(state.discussions, key = { it.id }) { msg ->
                    VCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(msg.studentName, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(msg.message, style = VTheme.type.body.colored(c.ink2))
                            Text(msg.createdAt, style = VTheme.type.caption.colored(c.ink3))
                        }
                    }
                }
            }
        } else {
            IllustratedEmptyState(title = "No discussions", body = "Enter a book ID to view and join discussions.", icon = VIcons.Chat)
        }

        VCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VInput(value = message, onValueChange = { message = it }, label = "Write a message", modifier = Modifier.fillMaxWidth())
                VButton(
                    text = "Post",
                    onClick = {
                        if (bookId.isNotBlank() && message.isNotBlank()) {
                            viewModel.postDiscussion(bookId, message)
                            message = ""
                        }
                    },
                    full = true,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Sm,
                    loading = state.isActionLoading,
                )
            }
        }
    }
}
