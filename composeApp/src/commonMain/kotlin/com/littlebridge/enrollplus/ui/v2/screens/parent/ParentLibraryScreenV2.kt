package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.library.domain.model.*
import com.littlebridge.enrollplus.feature.library.presentation.ParentLibraryState
import com.littlebridge.enrollplus.feature.library.presentation.ParentLibraryViewModel
import com.littlebridge.enrollplus.feature.parent.presentation.ParentDashboardViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VSnackbar
import com.littlebridge.enrollplus.ui.v2.components.VSnackbarTone
import com.littlebridge.enrollplus.ui.v2.components.ShimmerBox
import com.littlebridge.enrollplus.ui.v2.screens.VErrorState
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.screens.library.BookCover
import com.littlebridge.enrollplus.ui.v2.screens.library.BookShelfView
import com.littlebridge.enrollplus.ui.v2.screens.library.DueDateBadge
import com.littlebridge.enrollplus.ui.v2.screens.library.ExpandableSynopsis
import com.littlebridge.enrollplus.ui.v2.screens.library.GreetingHeader
import com.littlebridge.enrollplus.ui.v2.screens.library.IllustratedEmptyState
import com.littlebridge.enrollplus.ui.v2.screens.library.LibraryViewMode
import com.littlebridge.enrollplus.ui.v2.screens.library.QrShareDialog
import com.littlebridge.enrollplus.ui.v2.screens.library.ReadingTimeEstimate
import com.littlebridge.enrollplus.ui.v2.screens.library.ViewModeToggle
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

private enum class ParentLibraryTab(val label: String) {
    Browse("Browse"),
    MyBooks("My Books"),
    Reservations("Reservations"),
}

@Composable
fun ParentLibraryScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentLibraryViewModel = koinViewModel(),
    dashboardViewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val dashboard by dashboardViewModel.state.collectAsStateV2()
    var activeTab by remember { mutableStateOf(ParentLibraryTab.Browse) }
    val c = VTheme.colors
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    var showQrDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val childId = dashboard.selectedChild?.id

    LaunchedEffect(Unit) {
        viewModel.searchBooks(1)
        viewModel.loadReservations()
    }

    LaunchedEffect(childId) {
        if (childId != null) {
            viewModel.loadIssuedBooks(childId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Library", style = VTheme.type.h2.colored(c.ink))
                TextButton(onClick = onBack) { Text("Back") }
            }

            // Child selector hint
            if (dashboard.children.size > 1) {
                Text(
                    "Viewing books for ${dashboard.selectedChild?.name ?: "child"}",
                    style = VTheme.type.caption.colored(c.ink2),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ParentLibraryTab.entries.forEach { tab ->
                    VBadge(
                        text = tab.label,
                        tone = if (activeTab == tab) VBadgeTone.Accent else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { activeTab = tab },
                    )
                }
            }

            when (activeTab) {
                ParentLibraryTab.Browse -> ParentBrowseTab(state, viewModel, viewMode, { viewMode = it }, { id, title -> showQrDialog = id to title })
                ParentLibraryTab.MyBooks -> ParentMyBooksTab(state)
                ParentLibraryTab.Reservations -> ParentReservationsTab(state, viewModel)
            }
        }

        // Action message snackbar
        VSnackbar(
            message = state.actionMessage ?: "",
            visible = state.actionMessage != null,
            onDismiss = viewModel::clearActionMessage,
            tone = VSnackbarTone.Success,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        showQrDialog?.let { (bookId, title) ->
            QrShareDialog(bookId = bookId, bookTitle = title, onDismiss = { showQrDialog = null })
        }
    }
}

// ── Browse Tab ──────────────────────────────────────────────────────────────

@Composable
private fun ParentBrowseTab(
    state: ParentLibraryState,
    viewModel: ParentLibraryViewModel,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onShareQr: (String, String) -> Unit,
) {
    val c = VTheme.colors
    var reserveBookId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Greeting header (UIX-016)
        GreetingHeader(
            userName = "Parent",
            overdueCount = state.issuedBooks.count { com.littlebridge.enrollplus.ui.v2.screens.library.parseDaysRemaining(it.dueDate) < 0 },
            dueTomorrowCount = state.issuedBooks.count { com.littlebridge.enrollplus.ui.v2.screens.library.parseDaysRemaining(it.dueDate) in 0..1 },
            reservationReadyCount = state.reservations.count { it.status == "notified" },
        )

        VInput(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it); viewModel.searchBooks(1) },
            label = "Search by title, author, or ISBN",
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isLoading && state.books.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    VCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShimmerBox(width = 56.dp, height = 84.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ShimmerBox(height = 16.dp, modifier = Modifier.fillMaxWidth(0.7f))
                                ShimmerBox(height = 12.dp, modifier = Modifier.fillMaxWidth(0.5f))
                                ShimmerBox(height = 12.dp, modifier = Modifier.fillMaxWidth(0.3f))
                            }
                        }
                    }
                }
            }
            return@Column
        }

        if (state.error != null && state.books.isEmpty()) {
            VErrorState(message = state.error!!, onRetry = { viewModel.searchBooks(1) })
            return@Column
        }

        if (state.books.isEmpty()) {
            IllustratedEmptyState(title = "No books found", body = "Try a different search query.", icon = VIcons.Search)
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${state.booksTotal} books found", style = VTheme.type.caption.colored(c.ink2))
            ViewModeToggle(viewMode = viewMode, onModeChange = onViewModeChange)
        }

        if (viewMode == LibraryViewMode.SHELF) {
            BookShelfView(books = state.books, onBookClick = { })
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.books, key = { it.id }) { book ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BookCover(
                            title = book.title,
                            author = book.author,
                            coverUrl = book.coverUrl,
                            modifier = Modifier.size(56.dp, 84.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(book.title, style = VTheme.type.bodyStrong.colored(c.ink), maxLines = 2)
                            book.author?.let { Text(it, style = VTheme.type.caption.colored(c.ink2), maxLines = 1) }
                            book.category?.let { VBadge(text = it, tone = VBadgeTone.Neutral) }
                            ReadingTimeEstimate(pageCount = book.pageCount)
                            book.synopsis?.let { ExpandableSynopsis(synopsis = it) }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                VBadge(
                                    text = if (book.availableCopies > 0) "${book.availableCopies}/${book.totalCopies} available" else "Unavailable",
                                    tone = if (book.availableCopies > 0) VBadgeTone.Success else VBadgeTone.Danger,
                                )
                                if (book.availableCopies == 0) {
                                    VButton(
                                        text = "Reserve",
                                        onClick = { reserveBookId = book.id },
                                        tone = VButtonTone.Lavender,
                                        size = VButtonSize.Sm,
                                    )
                                }
                                VButton(
                                    text = "Share",
                                    onClick = { onShareQr(book.id, book.title) },
                                    variant = com.littlebridge.enrollplus.ui.v2.components.VButtonVariant.Secondary,
                                    tone = VButtonTone.Sand,
                                    size = VButtonSize.Sm,
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // Reserve confirmation dialog
        reserveBookId?.let { bookId ->
            AlertDialog(
                onDismissRequest = { reserveBookId = null },
                title = { Text("Reserve Book") },
                text = { Text("You'll be notified when this book becomes available.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.reserveBook(bookId)
                        reserveBookId = null
                    }) { Text("Reserve") }
                },
                dismissButton = { TextButton(onClick = { reserveBookId = null }) { Text("Cancel") } },
            )
        }
    }
}

// ── My Books Tab ────────────────────────────────────────────────────────────

@Composable
private fun ParentMyBooksTab(state: ParentLibraryState) {
    val c = VTheme.colors

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Child's Books", style = VTheme.type.h2.colored(c.ink))
        Text("Currently issued books for your child.", style = VTheme.type.caption.colored(c.ink2))

        if (state.issuedBooks.isEmpty()) {
            VEmptyState(title = "No books issued", body = "Your child has no books currently issued.", icon = VIcons.BookOpen)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.issuedBooks, key = { it.id }) { issue ->
                VCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(issue.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("Issued: ${issue.issueDate}", style = VTheme.type.caption.colored(c.ink2))
                        DueDateBadge(dueDate = issue.dueDate)
                        if (issue.renewalCount > 0) {
                            VBadge(text = "${issue.renewalCount} renewal(s)", tone = VBadgeTone.Neutral)
                        }
                        if (issue.fineAmount > 0) {
                            VBadge(
                                text = "Fine: ₹${"%.2f".format(issue.fineAmount)} (${issue.fineStatus})",
                                tone = VBadgeTone.Warning,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Reservations Tab ────────────────────────────────────────────────────────

@Composable
private fun ParentReservationsTab(state: ParentLibraryState, viewModel: ParentLibraryViewModel) {
    val c = VTheme.colors
    var cancelId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reservations", style = VTheme.type.h2.colored(c.ink))
        Text("Books you've reserved. You'll be notified when available.", style = VTheme.type.caption.colored(c.ink2))

        if (state.reservations.isEmpty()) {
            IllustratedEmptyState(title = "No reservations", body = "Reserve a book from the Browse tab to see it here.", icon = VIcons.Bookmark)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.reservations, key = { it.id }) { reservation ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(reservation.bookTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text("Reserved on: ${reservation.createdAt}", style = VTheme.type.caption.colored(c.ink2))
                            VBadge(
                                text = reservation.status.replaceFirstChar { it.uppercase() },
                                tone = when (reservation.status) {
                                    "pending" -> VBadgeTone.Warning
                                    "notified" -> VBadgeTone.Accent
                                    "fulfilled" -> VBadgeTone.Success
                                    "cancelled" -> VBadgeTone.Neutral
                                    else -> VBadgeTone.Neutral
                                },
                            )
                        }
                        if (reservation.status == "pending") {
                            VButton(
                                text = "Cancel",
                                onClick = { cancelId = reservation.id },
                                variant = com.littlebridge.enrollplus.ui.v2.components.VButtonVariant.Secondary,
                                tone = com.littlebridge.enrollplus.ui.v2.components.VButtonTone.Rose,
                                size = VButtonSize.Sm,
                            )
                        }
                    }
                }
            }
        }

        cancelId?.let { id ->
            AlertDialog(
                onDismissRequest = { cancelId = null },
                title = { Text("Cancel Reservation") },
                text = { Text("Are you sure you want to cancel this reservation?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.cancelReservation(id)
                        cancelId = null
                    }) { Text("Cancel Reservation") }
                },
                dismissButton = { TextButton(onClick = { cancelId = null }) { Text("Keep") } },
            )
        }
    }
}
