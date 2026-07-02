/*
 * File: LibraryJobScheduler.kt
 * Module: feature.library
 *
 * Scheduled background jobs for the Library Management feature.
 * Follows the NotificationScheduler / PulseWeeklyJob pattern: a long-running
 * coroutine launched at application startup that checks every hour whether
 * it's time to run, making it resilient to server restarts.
 *
 * Jobs:
 *   1. OverdueNotificationJob     — Daily 8 AM UTC: notify borrowers with overdue books
 *   2. DueDateReminderJob         — Daily 8 AM UTC: remind borrowers of upcoming due dates
 *   3. ReservationExpiryJob       — Daily midnight UTC: expire stale pending reservations
 *   4. AnnouncementExpiryJob      — Daily midnight UTC: deactivate expired announcements
 *   5. TrendingBooksRefreshJob    — Hourly: refresh trending book counts (cache warm)
 *   6. AuditLogRetentionJob       — 1st of month at midnight UTC: purge audit logs older than 2 years + verify hash chain
 *   7. BadgeCheckJob              — Daily 9 AM UTC: evaluate and award badges to all students
 *   8. ReservationPurgeJob        — Daily midnight UTC: purge fulfilled/cancelled reservations older than 1 year
 *   9. FinePurgeJob               — Daily midnight UTC: purge paid/waived fine records older than 3 years
 *  10. AnnouncementPurgeJob       — Daily midnight UTC: purge inactive announcements older than 1 year
 *  11. AcquisitionPurgeJob        — Daily midnight UTC: purge completed/rejected acquisition requests older than 2 years
 */
package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.feature.notifications.Notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.LocalDate
import java.util.UUID

object LibraryJobScheduler {
    private const val TAG = "LibraryJobScheduler"
    private val log = LoggerFactory.getLogger("LibraryJobScheduler")

    private val repo = LibraryRepository()
    private val service = LibraryService()

    @Volatile
    private var lastDailyRunDate: LocalDate? = null
    @Volatile
    private var lastMonthlyRunDate: LocalDate? = null
    @Volatile
    private var lastBadgeRunDate: LocalDate? = null
    @Volatile
    private var lastTrendingRefreshHour: Int = -1

    fun start(scope: CoroutineScope) {
        // Daily jobs — hourly check, fires at target hours
        scope.launch {
            log.info("[$TAG] Daily jobs started — overdue/due-date at 8 AM, expiry+purge at midnight, badges at 9 AM UTC")
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkAndRunDailyJobs() }
                    .onFailure { log.warn("[$TAG] Daily jobs failed: {}", it.message) }
                runCatching { checkAndRunBadgeJob() }
                    .onFailure { log.warn("[$TAG] Badge job failed: {}", it.message) }
            }
        }

        // Monthly audit log retention — hourly check, fires on 1st at midnight
        scope.launch {
            log.info("[$TAG] Monthly audit retention job started — 1st of month at midnight UTC")
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkAndRunMonthlyJob() }
                    .onFailure { log.warn("[$TAG] Monthly job failed: {}", it.message) }
            }
        }

        // Hourly trending refresh
        scope.launch {
            log.info("[$TAG] Trending refresh job started — hourly")
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { runTrendingRefresh() }
                    .onFailure { log.warn("[$TAG] Trending refresh failed: {}", it.message) }
            }
        }
    }

    // ── Daily jobs ──────────────────────────────────────────────────────────

    private suspend fun checkAndRunDailyJobs() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()

        // Guard: don't run twice in the same day
        if (lastDailyRunDate == today) return

        // Fire at either midnight (0) or 8 AM (8) UTC
        if (now.hour != 0 && now.hour != 8) return

        lastDailyRunDate = today
        log.info("[$TAG] Running daily jobs for {} (hour {} UTC)", today, now.hour)

        val schoolIds = runCatching { repo.listAllActiveSchoolIds() }
            .getOrElse { log.warn("[$TAG] Failed to list schools: {}", it.message); return }
        log.info("[$TAG] Processing {} schools", schoolIds.size)

        for (schoolId in schoolIds) {
            if (now.hour == 8) {
                runCatching { runOverdueNotifications(schoolId) }
                    .onFailure { log.warn("[$TAG] Overdue notifications failed for school {}: {}", schoolId, it.message) }
                runCatching { runDueDateReminders(schoolId) }
                    .onFailure { log.warn("[$TAG] Due-date reminders failed for school {}: {}", schoolId, it.message) }
            }
            if (now.hour == 0) {
                runCatching { runReservationExpiry(schoolId) }
                    .onFailure { log.warn("[$TAG] Reservation expiry failed for school {}: {}", schoolId, it.message) }
                runCatching { runAnnouncementExpiry(schoolId) }
                    .onFailure { log.warn("[$TAG] Announcement expiry failed for school {}: {}", schoolId, it.message) }
                runCatching { runReservationPurge(schoolId) }
                    .onFailure { log.warn("[$TAG] Reservation purge failed for school {}: {}", schoolId, it.message) }
                runCatching { runFinePurge(schoolId) }
                    .onFailure { log.warn("[$TAG] Fine purge failed for school {}: {}", schoolId, it.message) }
                runCatching { runAnnouncementPurge(schoolId) }
                    .onFailure { log.warn("[$TAG] Announcement purge failed for school {}: {}", schoolId, it.message) }
                runCatching { runAcquisitionPurge(schoolId) }
                    .onFailure { log.warn("[$TAG] Acquisition purge failed for school {}: {}", schoolId, it.message) }
            }
        }

        log.info("[$TAG] Daily jobs complete for {}", today)
    }

    /**
     * Notify borrowers who have overdue books.
     */
    private suspend fun runOverdueNotifications(schoolId: UUID) {
        val overdueIssues = repo.listOverdueIssues(schoolId)
        if (overdueIssues.isEmpty()) return

        for (issue in overdueIssues) {
            runCatching {
                val book = repo.findBookById(schoolId, issue.bookId)
                Notify.toUser(
                    userId = issue.borrowerId,
                    category = "library",
                    title = "📚 Book Overdue",
                    body = "'${book?.title ?: "Book"}' was due on ${issue.dueDate}. Please return it to avoid additional fines.",
                    schoolId = schoolId,
                    deepLink = "/student/library",
                    refType = "library_issue",
                    refId = issue.id.toString(),
                )
            }.onFailure { log.warn("[$TAG] Failed to notify borrower {}: {}", issue.borrowerId, it.message) }
        }

        log.info("[$TAG] Overdue notifications: sent {} for school {}", overdueIssues.size, schoolId)
    }

    /**
     * Remind borrowers whose books are due in the configured number of days
     * (from library settings, default 2).
     */
    private suspend fun runDueDateReminders(schoolId: UUID) {
        val settings = repo.getSettings(schoolId)
        val reminderDays = settings?.dueReminderDays ?: 2

        val upcomingIssues = repo.listIssuesDueInDays(schoolId, reminderDays)
        if (upcomingIssues.isEmpty()) return

        for (issue in upcomingIssues) {
            runCatching {
                val book = repo.findBookById(schoolId, issue.bookId)
                Notify.toUser(
                    userId = issue.borrowerId,
                    category = "library",
                    title = "📚 Book Due Soon",
                    body = "'${book?.title ?: "Book"}' is due on ${issue.dueDate}. Please return or renew it on time.",
                    schoolId = schoolId,
                    deepLink = "/student/library",
                    refType = "library_issue",
                    refId = issue.id.toString(),
                )
            }.onFailure { log.warn("[$TAG] Failed to remind borrower {}: {}", issue.borrowerId, it.message) }
        }

        log.info("[$TAG] Due-date reminders: sent {} for school {} ({} days ahead)", upcomingIssues.size, schoolId, reminderDays)
    }

    /**
     * Expire pending reservations older than the configured timeout.
     */
    private suspend fun runReservationExpiry(schoolId: UUID) {
        val settings = repo.getSettings(schoolId)
        val timeoutDays = settings?.reservationTimeoutDays ?: 7

        val expired = repo.expireStaleReservations(schoolId, timeoutDays)
        if (expired > 0) {
            log.info("[$TAG] Reservation expiry: {} expired for school {} (timeout {} days)", expired, schoolId, timeoutDays)
        }
    }

    /**
     * Deactivate announcements that have passed their expiry date.
     */
    private suspend fun runAnnouncementExpiry(schoolId: UUID) {
        val deactivated = repo.deactivateExpiredAnnouncements(schoolId)
        if (deactivated > 0) {
            log.info("[$TAG] Announcement expiry: {} deactivated for school {}", deactivated, schoolId)
        }
    }

    // ── Purge jobs (daily at midnight) ──────────────────────────────────────

    private suspend fun runReservationPurge(schoolId: UUID) {
        val deleted = repo.deleteOldReservations(schoolId, retentionDays = 365)
        if (deleted > 0) log.info("[$TAG] Reservation purge: {} deleted for school {}", deleted, schoolId)
    }

    private suspend fun runFinePurge(schoolId: UUID) {
        val deleted = repo.deleteOldFines(schoolId, retentionDays = 1095)
        if (deleted > 0) log.info("[$TAG] Fine purge: {} deleted for school {}", deleted, schoolId)
    }

    private suspend fun runAnnouncementPurge(schoolId: UUID) {
        val deleted = repo.deleteOldAnnouncements(schoolId, retentionDays = 365)
        if (deleted > 0) log.info("[$TAG] Announcement purge: {} deleted for school {}", deleted, schoolId)
    }

    private suspend fun runAcquisitionPurge(schoolId: UUID) {
        val deleted = repo.deleteOldAcquisitionRequests(schoolId, retentionDays = 730)
        if (deleted > 0) log.info("[$TAG] Acquisition purge: {} deleted for school {}", deleted, schoolId)
    }

    // ── Badge check job (daily at 9 AM) ─────────────────────────────────────

    private suspend fun checkAndRunBadgeJob() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()
        if (now.hour != 9) return
        if (lastBadgeRunDate == today) return
        lastBadgeRunDate = today

        log.info("[$TAG] Running badge check job for {}", today)
        val schoolIds = runCatching { repo.listAllActiveSchoolIds() }
            .getOrElse { log.warn("[$TAG] Failed to list schools: {}", it.message); return }

        for (schoolId in schoolIds) {
            runCatching {
                val borrowers = repo.listAllBorrowerIdsWithIssues(schoolId)
                for ((borrowerId, _) in borrowers) {
                    runCatching { service.checkAndAwardBadges(schoolId, borrowerId) }
                        .onFailure { log.warn("[$TAG] Badge check failed for borrower {}: {}", borrowerId, it.message) }
                }
            }.onFailure { log.warn("[$TAG] Badge check failed for school {}: {}", schoolId, it.message) }
        }
        log.info("[$TAG] Badge check job complete for {}", today)
    }

    // ── Hourly trending refresh ─────────────────────────────────────────────

    private suspend fun runTrendingRefresh() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        if (now.hour == lastTrendingRefreshHour) return
        lastTrendingRefreshHour = now.hour

        // Refresh materialized view on Postgres (spec §17)
        runCatching { repo.refreshTrendingMaterializedView() }
            .onFailure { log.warn("[$TAG] Materialized view refresh failed: {}", it.message) }

        val schoolIds = runCatching { repo.listAllActiveSchoolIds() }
            .getOrElse { log.warn("[$TAG] Failed to list schools for trending: {}", it.message); return }

        for (schoolId in schoolIds) {
            runCatching {
                val since = LocalDate.now().minusDays(30)
                val counts = repo.countIssuesSince(schoolId, since)
                log.debug("[$TAG] Trending refresh: {} books for school {} (30d)", counts.size, schoolId)
            }.onFailure { log.warn("[$TAG] Trending refresh failed for school {}: {}", schoolId, it.message) }
        }
    }

    // ── Monthly job ─────────────────────────────────────────────────────────

    private suspend fun checkAndRunMonthlyJob() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()

        // Guard: only on 1st of month at midnight
        if (today.dayOfMonth != 1) return
        if (now.hour != 0) return
        if (lastMonthlyRunDate == today) return
        lastMonthlyRunDate = today

        log.info("[$TAG] Running monthly retention jobs for {}", today)

        val schoolIds = runCatching { repo.listAllActiveSchoolIds() }
            .getOrElse { log.warn("[$TAG] Failed to list schools: {}", it.message); return }

        // ── Retention policy (spec §16 Data Retention) ────────────────────────
        //   audit log:      3 years (1095 days)
        //   issues:         5 years (1825 days) — only returned/closed
        //   announcements:  6 months (180 days) — already deactivated
        val AUDIT_RETENTION_DAYS = 1095
        val ISSUES_RETENTION_DAYS = 1825
        val ANNOUNCEMENT_RETENTION_DAYS = 180

        var totalAuditDeleted = 0
        var totalIssuesDeleted = 0
        var totalAnnouncementsDeleted = 0

        for (schoolId in schoolIds) {
            runCatching {
                // Audit log — verify hash chain before purging
                val verifyResult = service.verifyAuditHashChain(schoolId)
                if (!verifyResult.verified) {
                    log.warn("[$TAG] Audit hash chain BROKEN for school {} at entry {} — skipping purge", schoolId, verifyResult.brokenAt)
                    return@runCatching
                }
                val auditDeleted = repo.deleteOldAuditLogs(schoolId, AUDIT_RETENTION_DAYS)
                totalAuditDeleted += auditDeleted
                if (auditDeleted > 0) {
                    log.info("[$TAG] Audit retention: purged {} old logs for school {} ({}d)", auditDeleted, schoolId, AUDIT_RETENTION_DAYS)
                }
            }.onFailure { log.warn("[$TAG] Audit retention failed for school {}: {}", schoolId, it.message) }

            runCatching {
                val issuesDeleted = repo.deleteOldIssues(schoolId, ISSUES_RETENTION_DAYS)
                totalIssuesDeleted += issuesDeleted
                if (issuesDeleted > 0) {
                    log.info("[$TAG] Issues retention: purged {} old issues for school {} ({}d)", issuesDeleted, schoolId, ISSUES_RETENTION_DAYS)
                }
            }.onFailure { log.warn("[$TAG] Issues retention failed for school {}: {}", schoolId, it.message) }

            runCatching {
                val annDeleted = repo.deleteOldAnnouncements(schoolId, ANNOUNCEMENT_RETENTION_DAYS)
                totalAnnouncementsDeleted += annDeleted
                if (annDeleted > 0) {
                    log.info("[$TAG] Announcement retention: purged {} old announcements for school {} ({}d)", annDeleted, schoolId, ANNOUNCEMENT_RETENTION_DAYS)
                }
            }.onFailure { log.warn("[$TAG] Announcement retention failed for school {}: {}", schoolId, it.message) }
        }

        log.info("[$TAG] Monthly retention complete — audit:{} issues:{} announcements:{} across {} schools",
            totalAuditDeleted, totalIssuesDeleted, totalAnnouncementsDeleted, schoolIds.size)
    }

    /**
     * Manual trigger for testing / admin dev-tools.
     */
    suspend fun runNowDaily() {
        val schoolIds = repo.listAllActiveSchoolIds()
        for (schoolId in schoolIds) {
            runCatching { runOverdueNotifications(schoolId) }
            runCatching { runDueDateReminders(schoolId) }
            runCatching { runReservationExpiry(schoolId) }
            runCatching { runAnnouncementExpiry(schoolId) }
            runCatching { runReservationPurge(schoolId) }
            runCatching { runFinePurge(schoolId) }
            runCatching { runAnnouncementPurge(schoolId) }
            runCatching { runAcquisitionPurge(schoolId) }
        }
    }

    suspend fun runNowMonthly() {
        val schoolIds = repo.listAllActiveSchoolIds()
        for (schoolId in schoolIds) {
            runCatching { repo.deleteOldAuditLogs(schoolId, 730) }
        }
    }

    suspend fun runNowBadgeCheck() {
        val schoolIds = repo.listAllActiveSchoolIds()
        for (schoolId in schoolIds) {
            runCatching {
                val borrowers = repo.listAllBorrowerIdsWithIssues(schoolId)
                for ((borrowerId, _) in borrowers) {
                    runCatching { service.checkAndAwardBadges(schoolId, borrowerId) }
                }
            }
        }
    }

    suspend fun runNowTrendingRefresh() {
        val schoolIds = repo.listAllActiveSchoolIds()
        for (schoolId in schoolIds) {
            runCatching { repo.countIssuesSince(schoolId, LocalDate.now().minusDays(30)) }
        }
    }
}
