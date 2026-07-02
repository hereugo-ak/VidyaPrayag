package com.littlebridge.enrollplus.feature.notifications

import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.NotificationPreferencesTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class NotificationPreferenceDto(
    val category: String,
    val enabled: Boolean = true,
    val pushEnabled: Boolean? = null,
    val inAppEnabled: Boolean? = null,
    val emailEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val sound: String? = null
)

@Serializable
data class NotificationPreferencesResponse(
    val preferences: List<NotificationPreferenceDto>
)

@Serializable
data class UpdatePreferenceRequest(
    val category: String,
    val enabled: Boolean,
    val pushEnabled: Boolean? = null,
    val inAppEnabled: Boolean? = null,
    val emailEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val sound: String? = null
)

fun Route.notificationPreferencesRouting() {
    authenticate("jwt") {
        route("/api/v1/notifications/preferences") {

            // -------- list user's preferences --------
            get {
                val uid = call.principalUserUuid() ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@get
                }
                val prefs = dbQuery {
                    NotificationPreferencesTable.selectAll()
                        .where { NotificationPreferencesTable.userId eq uid }
                        .map {
                            NotificationPreferenceDto(
                                category = it[NotificationPreferencesTable.category],
                                enabled = it[NotificationPreferencesTable.enabled],
                                pushEnabled = it[NotificationPreferencesTable.pushEnabled],
                                inAppEnabled = it[NotificationPreferencesTable.inAppEnabled],
                                emailEnabled = it[NotificationPreferencesTable.emailEnabled],
                                smsEnabled = it[NotificationPreferencesTable.smsEnabled],
                                sound = it[NotificationPreferencesTable.sound]
                            )
                        }
                }
                call.ok(NotificationPreferencesResponse(preferences = prefs))
            }

            // -------- update a single preference --------
            put {
                val uid = call.principalUserUuid() ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@put
                }
                val req = runCatching { call.receive<UpdatePreferenceRequest>() }.getOrNull()
                    ?: run { call.respond(HttpStatusCode.BadRequest); return@put }

                val now = Instant.now()
                dbQuery {
                    val existing = NotificationPreferencesTable.selectAll()
                        .where {
                            (NotificationPreferencesTable.userId eq uid) and
                            (NotificationPreferencesTable.category eq req.category)
                        }
                        .singleOrNull()

                    if (existing == null) {
                        NotificationPreferencesTable.insert {
                            it[NotificationPreferencesTable.userId] = uid
                            it[NotificationPreferencesTable.category] = req.category
                            it[NotificationPreferencesTable.enabled] = req.enabled
                            it[NotificationPreferencesTable.pushEnabled] = req.pushEnabled
                            it[NotificationPreferencesTable.inAppEnabled] = req.inAppEnabled
                            it[NotificationPreferencesTable.emailEnabled] = req.emailEnabled
                            it[NotificationPreferencesTable.smsEnabled] = req.smsEnabled
                            it[NotificationPreferencesTable.sound] = req.sound
                            it[NotificationPreferencesTable.createdAt] = now
                            it[NotificationPreferencesTable.updatedAt] = now
                        }
                    } else {
                        NotificationPreferencesTable.update({
                            (NotificationPreferencesTable.userId eq uid) and
                            (NotificationPreferencesTable.category eq req.category)
                        }) {
                            it[NotificationPreferencesTable.enabled] = req.enabled
                            it[NotificationPreferencesTable.pushEnabled] = req.pushEnabled
                            it[NotificationPreferencesTable.inAppEnabled] = req.inAppEnabled
                            it[NotificationPreferencesTable.emailEnabled] = req.emailEnabled
                            it[NotificationPreferencesTable.smsEnabled] = req.smsEnabled
                            it[NotificationPreferencesTable.sound] = req.sound
                            it[NotificationPreferencesTable.updatedAt] = now
                        }
                    }
                }
                call.ok(
                    NotificationPreferenceDto(
                        category = req.category, enabled = req.enabled,
                        pushEnabled = req.pushEnabled, inAppEnabled = req.inAppEnabled,
                        emailEnabled = req.emailEnabled, smsEnabled = req.smsEnabled,
                        sound = req.sound,
                    ),
                    message = "Preference updated"
                )
            }

            // -------- reset (delete) a preference --------
            delete("/{category}") {
                val uid = call.principalUserUuid() ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@delete
                }
                val category = call.parameters["category"] ?: run {
                    call.respond(HttpStatusCode.BadRequest); return@delete
                }
                dbQuery {
                    NotificationPreferencesTable.deleteWhere {
                        (NotificationPreferencesTable.userId eq uid) and
                        (NotificationPreferencesTable.category eq category)
                    }
                }
                call.okMessage("Preference reset to default")
            }
        }
    }
}
