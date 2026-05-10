package com.littlebridge.vidyaprayag.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    fun init() {
        val dotenv = dotenv {
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
        
        val databaseUrl = dotenv["DATABASE_URL"] ?: System.getenv("DATABASE_URL")

        val dataSource = if (databaseUrl != null) {
            createPostgresDataSource(databaseUrl)
        } else {
            createSqliteDataSource()
        }

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(UserTable)
        }
    }

    private fun createPostgresDataSource(databaseUrl: String): HikariDataSource {
        val uri = URI(databaseUrl)
        val username = uri.userInfo.split(":")[0]
        val password = uri.userInfo.split(":")[1]
        val jdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
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
