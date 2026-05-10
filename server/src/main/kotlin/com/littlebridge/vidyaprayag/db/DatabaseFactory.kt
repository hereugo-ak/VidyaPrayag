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
        
        val databaseUrl = dotenv["DATABASE_URL"]?.takeIf { it.isNotBlank() } 
            ?: System.getenv("DATABASE_URL")?.takeIf { it.isNotBlank() }

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
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            config.jdbcUrl = databaseUrl
        } else {
            val uri = try {
                URI(databaseUrl)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid DATABASE_URL format: $databaseUrl. Expected postgresql://user:password@host:port/dbname", e)
            }
            
            val host = uri.host ?: throw IllegalArgumentException("DATABASE_URL is missing host information: $databaseUrl")
            val port = if (uri.port != -1) uri.port else 5432
            val path = uri.path ?: ""
            
            config.jdbcUrl = "jdbc:postgresql://$host:$port$path?sslmode=require"
            
            uri.userInfo?.let { userInfo ->
                if (":" in userInfo) {
                    config.username = userInfo.substringBefore(':')
                    config.password = userInfo.substringAfter(':')
                } else {
                    config.username = userInfo
                }
            } ?: throw IllegalArgumentException("DATABASE_URL is missing user info (username/password): $databaseUrl")
        }
        
        config.validate()
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
