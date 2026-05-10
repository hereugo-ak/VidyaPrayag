package com.littlebridge.vidyaprayag.db

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)
    val contact = varchar("contact", 255).uniqueIndex() // Primary identifier (email or phone)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 50).nullable()
    val passwordHash = varchar("password_hash", 255).nullable()
    val role = varchar("role", 50)
    val isPhoneVerified = bool("is_phone_verified").default(false)
    val isEmailVerified = bool("is_email_verified").default(false)

    override val primaryKey = PrimaryKey(id)
}
