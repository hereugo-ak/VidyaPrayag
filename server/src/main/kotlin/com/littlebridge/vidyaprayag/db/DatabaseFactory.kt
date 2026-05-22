/*
 * File: DatabaseFactory.kt
 * Module: db
 *
 * Connects the Ktor backend to the configured database:
 *   - PRODUCTION / STAGING : Supabase Postgres (DATABASE_URL set)
 *   - LOCAL DEV (default)  : SQLite file `data.db` in CWD
 *
 * IMPORTANT: against Postgres we DO NOT run any schema migration from code.
 * The source of truth is two SQL files run manually in Supabase SQL Editor:
 *     1.  /supabase_schema                              (operational tables)
 *     2.  /docs/backend/sql/01_supplementary_schema.sql (this backend's tables)
 *
 * Why?  Letting an ORM mutate production schema silently is a recipe for
 * downtime.  All schema changes go through a reviewed SQL migration PR
 * and are executed by a human in the Supabase dashboard.
 *
 * Against SQLite (no DATABASE_URL), we *do* call
 * SchemaUtils.createMissingTablesAndColumns(...) so the server boots on
 * a fresh clone with zero setup.
 *
 * ENVIRONMENT VARIABLES READ
 *   DATABASE_URL       : full JDBC or postgres:// URL
 *   DATABASE_USER      : Postgres user (optional if encoded in URL)
 *   DATABASE_PASSWORD  : Postgres password (optional if encoded in URL)
 *   DB_POOL_SIZE       : HikariCP pool size (default 5)
 *   APP_SEED_CMS       : "true" to seed/upsert landing+app_config rows
 *                        (default "true" — these are CMS strings, safe to seed)
 */
package com.littlebridge.vidyaprayag.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    /** All tables the backend reads/writes. Order matters for SQLite FKs. */
    private val allTables = arrayOf(
        AppUsersTable,
        AuthOtpsTable,
        UserSessionsTable,
        LandingContentTable,
        AppConfigTable,
        SchoolsTable,
        OnboardingDraftsTable,
        SchoolClassesTable,
        SchoolSubjectsTable,
        AnnouncementsTable,
        WhatsappLogsTable,
        AdmissionEnquiriesTable,
        SchoolPhilosophyTable,
        SchoolMediaTable,
        StorageMetricsTable,
        AcademicCalendarTable,
        HolidayListTable,
        FacultyTable,
        AttendanceRecordsTable,
        StudentsTable
    )

    /** True when DATABASE_URL is set → we're talking to Postgres / Supabase. */
    var isPostgres: Boolean = false
        private set

    fun init() {
        val dotenv = dotenv {
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        val databaseUrl = (dotenv["DATABASE_URL"] ?: System.getenv("DATABASE_URL"))
            ?.takeIf { it.isNotBlank() }

        val dataSource = if (databaseUrl != null) {
            isPostgres = true
            createPostgresDataSource(
                databaseUrl,
                user = dotenv["DATABASE_USER"] ?: System.getenv("DATABASE_USER"),
                password = dotenv["DATABASE_PASSWORD"] ?: System.getenv("DATABASE_PASSWORD"),
                poolSize = (dotenv["DB_POOL_SIZE"] ?: System.getenv("DB_POOL_SIZE"))
                    ?.toIntOrNull() ?: 5
            )
        } else {
            createSqliteDataSource()
        }

        Database.connect(dataSource)

        if (!isPostgres) {
            // Local-dev convenience only. Never touch production schema.
            transaction {
                SchemaUtils.createMissingTablesAndColumns(*allTables)
            }
        }

        // CMS seed (landing + app_config). Always idempotent — only inserts
        // missing keys; never overwrites operator-edited values.
        val seedCms = (dotenv["APP_SEED_CMS"] ?: System.getenv("APP_SEED_CMS") ?: "true")
            .equals("true", ignoreCase = true)
        if (seedCms) {
            CmsSeed.ensureLandingAndConfig()
        }
    }

    private fun createPostgresDataSource(
        databaseUrl: String,
        user: String?,
        password: String?,
        poolSize: Int
    ): HikariDataSource {
        // Accept both forms:
        //   postgresql://USER:PASS@HOST:5432/DB?sslmode=require
        //   jdbc:postgresql://HOST:5432/DB?sslmode=require
        //   postgres://USER:PASS@HOST:5432/DB
        val jdbcUrl = when {
            databaseUrl.startsWith("jdbc:") -> databaseUrl
            databaseUrl.startsWith("postgres://") ->
                "jdbc:" + databaseUrl.replaceFirst("postgres://", "postgresql://")
            databaseUrl.startsWith("postgresql://") -> "jdbc:$databaseUrl"
            else -> "jdbc:postgresql://$databaseUrl"
        }

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
            if (!user.isNullOrBlank()) this.username = user
            if (!password.isNullOrBlank()) this.password = password
            maximumPoolSize = poolSize
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            // Sensible defaults for Supabase (pooled, IPv4 PgBouncer port 6543).
            addDataSourceProperty("ApplicationName", "vidyaprayag-ktor")
            addDataSourceProperty("reWriteBatchedInserts", "true")
            connectionTimeout = 30_000
            validationTimeout = 5_000
            maxLifetime = 30 * 60 * 1000L
            validate()
        }
        return HikariDataSource(config)
    }

    private fun createSqliteDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:data.db"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_SERIALIZABLE"
            validate()
        }
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
