package com.littlebridge.enrollplus.feature.notifications

import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.FeeRecordsTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkFeeReminders() }
                    .onFailure { println("[$TAG] checkFeeReminders failed: ${it.message}") }
                runCatching { checkCalendarReminders() }
                    .onFailure { println("[$TAG] checkCalendarReminders failed: ${it.message}") }
            }
        }
    }

    suspend fun checkFeeReminders() {
        val now = Instant.now()
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)
        val today = LocalDate.now().toString()

        val dueFees = dbQuery {
            FeeRecordsTable.selectAll()
                .where {
                    (FeeRecordsTable.status inList listOf("DUE", "OVERDUE")) and
                    (FeeRecordsTable.dueDate lessEq today) and
                    (FeeRecordsTable.lastRemindedAt.isNull() or (FeeRecordsTable.lastRemindedAt less oneDayAgo))
                }.toList()
        }

        if (dueFees.isEmpty()) return

        for (row in dueFees) {
            val parentId = row[FeeRecordsTable.parentId]
            val feeId = row[FeeRecordsTable.id].value
            val status = row[FeeRecordsTable.status]
            val dueDate = row[FeeRecordsTable.dueDate]
            val amount = row[FeeRecordsTable.amount]
            val currency = row[FeeRecordsTable.currency]
            val title = row[FeeRecordsTable.title]

            val notifTitle = if (status == "OVERDUE") "Fee Overdue" else "Fee Due Soon"
            val notifBody = "$currency $amount — '$title' due $dueDate"

            Notify.toUser(
                userId = parentId,
                category = "fees",
                title = notifTitle,
                body = notifBody,
                schoolId = row[FeeRecordsTable.schoolId],
                deepLink = "/parent/fees",
                refType = "fee_record",
                refId = feeId.toString(),
            )

            dbQuery {
                FeeRecordsTable.update({ FeeRecordsTable.id eq feeId }) {
                    it[lastRemindedAt] = now
                }
            }
        }

        println("[$TAG] checkFeeReminders: sent ${dueFees.size} reminders")
    }

    suspend fun checkCalendarReminders() {
        val tomorrow = LocalDate.now().plusDays(1)

        val upcomingEvents = dbQuery {
            CalendarEventsTable.selectAll()
                .where {
                    (CalendarEventsTable.status eq "PUBLISHED") and
                    (CalendarEventsTable.startDate eq tomorrow) and
                    (CalendarEventsTable.reminderSent eq false)
                }.toList()
        }

        if (upcomingEvents.isEmpty()) return

        for (row in upcomingEvents) {
            val schoolId = row[CalendarEventsTable.schoolId]
            val eventId = row[CalendarEventsTable.id].value
            val eventTitle = row[CalendarEventsTable.title]
            val eventDate = row[CalendarEventsTable.startDate]

            val parentUserIds = NotifyRecipients.parentsInSchool(schoolId)
            if (parentUserIds.isNotEmpty()) {
                Notify.toUsers(
                    userIds = parentUserIds,
                    category = "calendar",
                    title = "Upcoming: $eventTitle",
                    body = "Event on $eventDate",
                    schoolId = schoolId,
                    deepLink = "/parent/calendar",
                    refType = "calendar_event",
                    refId = eventId.toString(),
                )
            }

            dbQuery {
                CalendarEventsTable.update({ CalendarEventsTable.id eq eventId }) {
                    it[reminderSent] = true
                }
            }
        }

        println("[$TAG] checkCalendarReminders: sent ${upcomingEvents.size} reminders")
    }
}
