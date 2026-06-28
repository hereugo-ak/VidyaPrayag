/*
 * File: BrandingService.kt
 * Module: feature.branding
 *
 * Core service for the School Branding Kit (SCHOOL_BRANDING_KIT_SPEC.md).
 *
 * Handles:
 *   - Branding CRUD (get/update per school)
 *   - Color validation (hex format)
 *   - Subdomain management (check availability, assign, remove)
 *   - Brand asset URL storage (logos, icons, splash — upload handled by routing layer)
 *   - Default fallback when no branding row exists
 *
 * Plug-and-play: routing calls this service, no direct DB access from routing layer.
 */
package com.littlebridge.enrollplus.feature.branding

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.SchoolBrandingTable
import com.littlebridge.enrollplus.db.SchoolsTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class SchoolBrandingDto(
    val schoolId: String,
    val schoolName: String,
    val logoUrl: String? = null,
    val logoDarkUrl: String? = null,
    val faviconUrl: String? = null,
    val appIconUrl: String? = null,
    val splashScreenUrl: String? = null,
    val primaryColor: String = "#2563EB",
    val secondaryColor: String = "#1E40AF",
    val accentColor: String = "#3B82F6",
    val customSubdomain: String? = null,
    val loginBackgroundUrl: String? = null,
    val isCustomized: Boolean = false,
)

@Serializable
data class UpdateBrandingRequest(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val accentColor: String? = null,
    val logoUrl: String? = null,
    val logoDarkUrl: String? = null,
    val faviconUrl: String? = null,
    val appIconUrl: String? = null,
    val splashScreenUrl: String? = null,
    val loginBackgroundUrl: String? = null,
    val isCustomized: Boolean? = null,
)

@Serializable
data class SubdomainRequest(
    val subdomain: String,
)

@Serializable
data class SubdomainResponse(
    val subdomain: String,
)

@Serializable
data class SubdomainResolutionDto(
    val schoolId: String,
    val schoolName: String,
    val branding: SchoolBrandingDto,
)

// ── Service ──────────────────────────────────────────────────────────────────

class BrandingService {

    private val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
    private val subdomainRegex = SUBDOMAIN_REGEX

    companion object {
        val SUBDOMAIN_REGEX = Regex("^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$")
    }

    // ── Read ──────────────────────────────────────────────────────────────

    suspend fun getBranding(schoolId: UUID): SchoolBrandingDto = dbQuery {
        val schoolName = SchoolsTable.selectAll()
            .where { SchoolsTable.id eq schoolId }
            .singleOrNull()?.get(SchoolsTable.name) ?: "Unknown School"

        val row = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        if (row != null) {
            rowToDto(row, schoolName)
        } else {
            // EC-1: No branding row → return defaults
            defaultDto(schoolId, schoolName)
        }
    }

    // ── Public read (no auth) ─────────────────────────────────────────────

    suspend fun getPublicBranding(schoolId: UUID): SchoolBrandingDto? = dbQuery {
        val schoolRow = SchoolsTable.selectAll()
            .where { (SchoolsTable.id eq schoolId) and (SchoolsTable.isActive eq true) }
            .singleOrNull() ?: return@dbQuery null

        val schoolName = schoolRow[SchoolsTable.name]
        val row = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        if (row != null) rowToDto(row, schoolName) else defaultDto(schoolId, schoolName)
    }

    // ── Update ────────────────────────────────────────────────────────────

    suspend fun updateBranding(schoolId: UUID, req: UpdateBrandingRequest): SchoolBrandingDto = dbQuery {
        // Validate colors if provided
        req.primaryColor?.let { requireHex(it, "primary_color") }
        req.secondaryColor?.let { requireHex(it, "secondary_color") }
        req.accentColor?.let { requireHex(it, "accent_color") }

        // Validate URL fields are non-empty if provided
        req.logoUrl?.let { requireNonEmptyUrl(it, "logo_url") }
        req.logoDarkUrl?.let { requireNonEmptyUrl(it, "logo_dark_url") }
        req.faviconUrl?.let { requireNonEmptyUrl(it, "favicon_url") }
        req.appIconUrl?.let { requireNonEmptyUrl(it, "app_icon_url") }
        req.splashScreenUrl?.let { requireNonEmptyUrl(it, "splash_screen_url") }
        req.loginBackgroundUrl?.let { requireNonEmptyUrl(it, "login_background_url") }

        val now = Instant.now()
        val existing = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        if (existing != null) {
            SchoolBrandingTable.update(
                { SchoolBrandingTable.schoolId eq schoolId }
            ) {
                req.primaryColor?.let { c -> it[primaryColor] = c }
                req.secondaryColor?.let { c -> it[secondaryColor] = c }
                req.accentColor?.let { c -> it[accentColor] = c }
                req.logoUrl?.let { u -> it[logoUrl] = u }
                req.logoDarkUrl?.let { u -> it[logoDarkUrl] = u }
                req.faviconUrl?.let { u -> it[faviconUrl] = u }
                req.appIconUrl?.let { u -> it[appIconUrl] = u }
                req.splashScreenUrl?.let { u -> it[splashScreenUrl] = u }
                req.loginBackgroundUrl?.let { u -> it[loginBackgroundUrl] = u }
                req.isCustomized?.let { flag -> it[isCustomized] = flag }
                it[updatedAt] = now
            }
        } else {
            SchoolBrandingTable.insert {
                it[SchoolBrandingTable.schoolId] = schoolId
                req.primaryColor?.let { c -> it[primaryColor] = c }
                req.secondaryColor?.let { c -> it[secondaryColor] = c }
                req.accentColor?.let { c -> it[accentColor] = c }
                req.logoUrl?.let { u -> it[logoUrl] = u }
                req.logoDarkUrl?.let { u -> it[logoDarkUrl] = u }
                req.faviconUrl?.let { u -> it[faviconUrl] = u }
                req.appIconUrl?.let { u -> it[appIconUrl] = u }
                req.splashScreenUrl?.let { u -> it[splashScreenUrl] = u }
                req.loginBackgroundUrl?.let { u -> it[loginBackgroundUrl] = u }
                it[isCustomized] = req.isCustomized ?: true
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        val schoolName = SchoolsTable.selectAll()
            .where { SchoolsTable.id eq schoolId }
            .singleOrNull()?.get(SchoolsTable.name) ?: "Unknown School"

        SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .single().let { rowToDto(it, schoolName) }
    }

    // ── Reset to default ──────────────────────────────────────────────────

    suspend fun resetBranding(schoolId: UUID): SchoolBrandingDto = dbQuery {
        val now = Instant.now()
        val existing = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        if (existing != null) {
            SchoolBrandingTable.update(
                { SchoolBrandingTable.schoolId eq schoolId }
            ) {
                it[primaryColor] = "#2563EB"
                it[secondaryColor] = "#1E40AF"
                it[accentColor] = "#3B82F6"
                it[isCustomized] = false
                it[updatedAt] = now
            }
        } else {
            SchoolBrandingTable.insert {
                it[SchoolBrandingTable.schoolId] = schoolId
                it[primaryColor] = "#2563EB"
                it[secondaryColor] = "#1E40AF"
                it[accentColor] = "#3B82F6"
                it[isCustomized] = false
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        val schoolName = SchoolsTable.selectAll()
            .where { SchoolsTable.id eq schoolId }
            .singleOrNull()?.get(SchoolsTable.name) ?: "Unknown School"

        val row = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .single()

        rowToDto(row, schoolName)
    }

    // ── Subdomain ─────────────────────────────────────────────────────────

    suspend fun checkSubdomainAvailable(schoolId: UUID, subdomain: String): Boolean = dbQuery {
        if (!subdomainRegex.matches(subdomain)) return@dbQuery false
        val existing = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.customSubdomain eq subdomain }
            .singleOrNull()
        existing == null || existing[SchoolBrandingTable.schoolId] == schoolId
    }

    suspend fun updateSubdomain(schoolId: UUID, subdomain: String): SubdomainResponse = dbQuery {
        if (!subdomainRegex.matches(subdomain)) {
            throw IllegalArgumentException("Invalid subdomain format")
        }
        // Check uniqueness
        val taken = SchoolBrandingTable.selectAll()
            .where {
                (SchoolBrandingTable.customSubdomain eq subdomain) and
                    (SchoolBrandingTable.schoolId neq schoolId)
            }
            .singleOrNull()
        if (taken != null) {
            throw IllegalStateException("Subdomain already taken")
        }

        val now = Instant.now()
        val existing = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        if (existing != null) {
            SchoolBrandingTable.update(
                { SchoolBrandingTable.schoolId eq schoolId }
            ) {
                it[customSubdomain] = subdomain
                it[isCustomized] = true
                it[updatedAt] = now
            }
        } else {
            SchoolBrandingTable.insert {
                it[SchoolBrandingTable.schoolId] = schoolId
                it[customSubdomain] = subdomain
                it[isCustomized] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        SubdomainResponse(subdomain)
    }

    suspend fun removeSubdomain(schoolId: UUID): Boolean = dbQuery {
        val now = Instant.now()
        val updated = SchoolBrandingTable.update(
            { SchoolBrandingTable.schoolId eq schoolId }
        ) {
            it[customSubdomain] = null
            it[updatedAt] = now
        }
        updated > 0
    }

    suspend fun resolveSubdomain(subdomain: String): SubdomainResolutionDto? = dbQuery {
        val row = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.customSubdomain eq subdomain }
            .singleOrNull() ?: return@dbQuery null

        val schoolId = row[SchoolBrandingTable.schoolId]
        val schoolRow = SchoolsTable.selectAll()
            .where { (SchoolsTable.id eq schoolId) and (SchoolsTable.isActive eq true) }
            .singleOrNull() ?: return@dbQuery null

        val schoolName = schoolRow[SchoolsTable.name]

        SubdomainResolutionDto(
            schoolId = schoolId.toString(),
            schoolName = schoolName,
            branding = rowToDto(row, schoolName),
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun requireHex(value: String, field: String) {
        if (!hexRegex.matches(value)) {
            throw IllegalArgumentException("Invalid hex color for $field: $value")
        }
    }

    private fun requireNonEmptyUrl(value: String, field: String) {
        if (value.isBlank()) {
            throw IllegalArgumentException("URL for $field cannot be empty")
        }
    }

    private fun rowToDto(row: ResultRow, schoolName: String): SchoolBrandingDto = SchoolBrandingDto(
        schoolId = row[SchoolBrandingTable.schoolId].toString(),
        schoolName = schoolName,
        logoUrl = row[SchoolBrandingTable.logoUrl],
        logoDarkUrl = row[SchoolBrandingTable.logoDarkUrl],
        faviconUrl = row[SchoolBrandingTable.faviconUrl],
        appIconUrl = row[SchoolBrandingTable.appIconUrl],
        splashScreenUrl = row[SchoolBrandingTable.splashScreenUrl],
        primaryColor = row[SchoolBrandingTable.primaryColor],
        secondaryColor = row[SchoolBrandingTable.secondaryColor],
        accentColor = row[SchoolBrandingTable.accentColor],
        customSubdomain = row[SchoolBrandingTable.customSubdomain],
        loginBackgroundUrl = row[SchoolBrandingTable.loginBackgroundUrl],
        isCustomized = row[SchoolBrandingTable.isCustomized],
    )

    private fun defaultDto(schoolId: UUID, schoolName: String): SchoolBrandingDto = SchoolBrandingDto(
        schoolId = schoolId.toString(),
        schoolName = schoolName,
        primaryColor = "#2563EB",
        secondaryColor = "#1E40AF",
        accentColor = "#3B82F6",
        isCustomized = false,
    )
}
