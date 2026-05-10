package com.littlebridge.vidyaprayag.db

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)
    val contact = varchar("contact", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = varchar("role", 50)

    override val primaryKey = PrimaryKey(id)
}
