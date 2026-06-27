# Two-Factor Authentication — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None (extends existing OTP auth system)

---

## 1. Feature Overview

Time-based One-Time Password (TOTP) second factor for school admin accounts, using authenticator apps (Google Authenticator, Microsoft Authenticator, Authy). Extends the existing JWT auth flow with an optional 2FA step.

### Goals

- Admin can enable 2FA via TOTP (RFC 6238)
- QR code provisioning for authenticator app enrollment
- Backup codes for recovery
- Enforce 2FA for school admin role (configurable per school)
- Graceful fallback: if 2FA device lost, admin can use backup code or request super admin reset

---

## 2. Current System Assessment

- `feature_audit.csv` L149: "🔴 Missing, 0%"
- Existing OTP system (`AuthOtpsTable`, `OtpService`) is SMS/WhatsApp-based for login — separate from TOTP
- `AppUsersTable` has `role` field (school_admin, teacher, parent)
- JWT auth flow: phone+OTP → JWT access+refresh tokens
- No TOTP library, no 2FA table

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No TOTP enrollment | Cannot set up 2FA |
| G2 | No 2FA verification step | No second factor check |
| G3 | No backup codes | Account lockout if device lost |
| G4 | No 2FA enforcement | Admins can skip 2FA |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin can enable 2FA: generate TOTP secret, display QR code |
| FR-2 | Admin verifies 2FA by entering 6-digit code from authenticator app |
| FR-3 | Generate 10 single-use backup codes on enrollment |
| FR-4 | Login flow: after OTP verification, if 2FA enabled, prompt for TOTP code |
| FR-5 | Backup code accepted as alternative to TOTP code |
| FR-6 | Admin can disable 2FA (requires current TOTP code) |
| FR-7 | Super admin can reset 2FA for a locked-out admin |
| FR-8 | School can enforce 2FA for all admin accounts (config flag) |
| FR-9 | TOTP secret stored encrypted (AES-256) |

---

## 5. User Roles & Permissions

| Action | School Admin | Super Admin | Teacher/Parent |
|---|---|---|---|
| Enable 2FA on own account | ✅ | ✅ | ✅ (optional) |
| Disable 2FA on own account | ✅ | ✅ | ✅ |
| Reset 2FA for another admin | ❌ | ✅ | ❌ |
| Enforce 2FA for school | ❌ | ✅ | ❌ |

---

## 6. Database Design

### 6.1 New Table: `two_factor_auth`

```sql
CREATE TABLE two_factor_auth (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,          -- FK app_users.id
    totp_secret_encrypted TEXT NOT NULL,           -- AES-256 encrypted base32 secret
    is_enabled      BOOLEAN NOT NULL DEFAULT false,
    enrolled_at     TIMESTAMP,
    backup_codes_hash TEXT NOT NULL,               -- bcrypt-hashed JSON array of backup codes
    backup_codes_used TEXT NOT NULL DEFAULT '[]',  -- JSON array of used code indices
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.2 Modify Existing: `app_users`

```sql
ALTER TABLE app_users ADD COLUMN two_factor_required BOOLEAN NOT NULL DEFAULT false;
```

### 6.3 Modify Existing: `schools`

```sql
ALTER TABLE schools ADD COLUMN enforce_2fa_for_admins BOOLEAN NOT NULL DEFAULT false;
```

### 6.4 Exposed Mappings

```kotlin
object TwoFactorAuthTable : UUIDTable("two_factor_auth", "id") {
    val userId              = uuid("user_id").uniqueIndex()
    val totpSecretEncrypted = text("totp_secret_encrypted")
    val isEnabled           = bool("is_enabled").default(false)
    val enrolledAt          = timestamp("enrolled_at").nullable()
    val backupCodesHash     = text("backup_codes_hash")
    val backupCodesUsed     = text("backup_codes_used").default("[]")
    val lastUsedAt          = timestamp("last_used_at").nullable()
    val createdAt           = timestamp("created_at")
    val updatedAt           = timestamp("updated_at")
}
```

---

## 7. Backend Architecture

### 7.1 TwoFactorService

```kotlin
class TwoFactorService(
    private val encryptionService: EncryptionService,
    private val totpGenerator: TotpGenerator
) {
    suspend fun enroll(userId: UUID): TotpEnrollmentDto {
        // 1. Generate random base32 secret (20 bytes)
        val secret = Base32.encode(ByteArray(20).also { SecureRandom().nextBytes(it) })
        // 2. Encrypt and store
        val encrypted = encryptionService.encrypt(secret)
        // 3. Generate otpauth URI for QR code
        val uri = "otpauth://totp/VidyaPrayag:${userEmail}?secret=$secret&issuer=VidyaPrayag"
        // 4. Generate 10 backup codes
        val backupCodes = (1..10).map { generateBackupCode() }
        // 5. Store hash of backup codes
        // 6. Return QR URI + backup codes (shown once)
        return TotpEnrollmentDto(qrUri = uri, backupCodes = backupCodes)
    }

    suspend fun verify(userId: UUID, code: String): Boolean {
        val tfa = repository.get(userId)
        val secret = encryptionService.decrypt(tfa.totpSecretEncrypted)
        // Check TOTP code (±1 time step for clock skew)
        if (totpGenerator.verify(secret, code)) return true
        // Check backup codes
        if (verifyBackupCode(tfa, code)) return true
        return false
    }

    suspend fun enable(userId: UUID, verificationCode: String): Boolean
    suspend fun disable(userId: UUID, verificationCode: String): Boolean
    suspend fun reset(userId: UUID, adminId: UUID): Unit  // super admin only
}
```

### 7.2 TotpGenerator

```kotlin
class TotpGenerator {
    fun generate(secret: String, time: Long = System.currentTimeMillis() / 1000): String {
        // RFC 6238 TOTP
        // HMAC-SHA1, 6 digits, 30-second period
    }

    fun verify(secret: String, code: String, time: Long = System.currentTimeMillis() / 1000): Boolean {
        // Check current, previous, and next time step (±30s clock skew)
        val currentCode = generate(secret, time)
        val prevCode = generate(secret, time - 30)
        val nextCode = generate(secret, time + 30)
        return code == currentCode || code == prevCode || code == nextCode
    }
}
```

### 7.3 Login Flow Modification

```
1. Phone + OTP → verify (existing)
2. If user.two_factor_required OR school.enforce_2fa_for_admins:
   a. Check if 2FA enrolled
   b. If not enrolled → return 2FA_ENROLLMENT_REQUIRED (force enrollment)
   c. If enrolled → return 2FA_CHALLENGE (don't issue JWT yet)
3. POST /auth/2fa/verify { code } → verify TOTP → issue JWT
```

### 7.4 New Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `dev.samstevens.totp:totp` | 1.7.1 | TOTP generation/verification |

---

## 8. API Contracts

### 8.1 Enroll 2FA

```
POST /api/v1/school/2fa/enroll
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "qr_uri": "otpauth://totp/VidyaPrayag:admin@school.com?secret=JBSWY3DPEHPK3PXP&issuer=VidyaPrayag",
    "backup_codes": ["12345678", "87654321", ...]
  }
}
```

### 8.2 Verify and Enable

```
POST /api/v1/school/2fa/enable
{
  "code": "123456"
}
```

### 8.3 Login 2FA Challenge

```
POST /api/v1/auth/login
{ "phone": "...", "otp": "..." }
```

**Response 200 (2FA required):**
```json
{
  "success": true,
  "data": {
    "status": "2FA_CHALLENGE",
    "challenge_token": "temp-uuid"  // short-lived token for 2FA verification
  }
}
```

### 8.4 Verify 2FA at Login

```
POST /api/v1/auth/2fa/verify
{
  "challenge_token": "temp-uuid",
  "code": "123456"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "access_token": "...",
    "refresh_token": "..."
  }
}
```

### 8.5 Disable 2FA

```
POST /api/v1/school/2fa/disable
{
  "code": "123456"
}
```

### 8.6 Reset 2FA (Super Admin)

```
POST /api/v1/super/2fa/reset
{
  "user_id": "uuid"
}
```

---

## 9. Security

- TOTP secret encrypted at rest (AES-256-GCM)
- Backup codes hashed with bcrypt (never stored in plaintext)
- Challenge token is short-lived (5 min) and single-use
- TOTP verification accepts ±1 time step (±30s clock skew)
- Rate limiting: max 5 failed 2FA attempts per 5 minutes → lockout 15 min
- Reset by super admin is logged in audit log

---

## 10. Testing Strategy

### 10.1 Unit Tests

- TOTP generation matches RFC 6238 test vectors
- TOTP verification with ±1 time step
- Backup code generation, hashing, and single-use enforcement
- Encryption/decryption of TOTP secret

### 10.2 Integration Tests

- Enroll → verify → enable → login requires 2FA → verify → JWT issued
- Backup code used at login → accepted, marked as used
- Backup code reused → rejected
- Disable 2FA → login no longer requires 2FA
- School enforces 2FA → admin without 2FA forced to enroll
- Rate limiting: 5 failed attempts → lockout
- Super admin reset → admin can re-enroll

---

## 11. Acceptance Criteria

- [ ] Admin can enroll 2FA via QR code + authenticator app
- [ ] 10 backup codes generated and shown once
- [ ] Login flow requires 2FA code after OTP verification
- [ ] Backup codes accepted as alternative to TOTP
- [ ] Backup codes are single-use
- [ ] Admin can disable 2FA (requires current code)
- [ ] Super admin can reset 2FA for locked-out admin
- [ ] School can enforce 2FA for all admin accounts
- [ ] Rate limiting prevents brute force
- [ ] TOTP secret encrypted at rest

---

## 12. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | TotpGenerator + TwoFactorService |
| 3 | 2 days | Login flow modification (2FA challenge) |
| 4 | 1 day | Rate limiting for 2FA attempts |
| 5 | 2 days | API endpoints (enroll, enable, disable, verify, reset) |
| 6 | 2 days | Client UI (QR code display, 2FA code input, backup codes display) |
| 7 | 2 days | Tests (unit + integration) |

---

## 13. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `TwoFactorAuthTable` + columns on app_users, schools |
| `server/.../feature/auth/TwoFactorService.kt` | New | 2FA enrollment, verification, backup codes |
| `server/.../feature/auth/TotpGenerator.kt` | New | TOTP RFC 6238 implementation |
| `server/.../feature/auth/AuthRouting.kt` | Modify | Add 2FA challenge + verify endpoints |
| `server/.../feature/auth/AuthService.kt` | Modify | Add 2FA check in login flow |
| `server/build.gradle.kts` | Modify | Add totp dependency |
| `docs/db/migration_038_two_factor_auth.sql` | New | DDL |
| `shared/.../feature/auth/AuthApi.kt` | Modify | Add 2FA endpoints |
| `composeApp/.../ui/v2/screens/auth/TwoFactorEnrollScreen.kt` | New | QR code + backup codes UI |
| `composeApp/.../ui/v2/screens/auth/TwoFactorVerifyScreen.kt` | New | 2FA code input at login |
| `composeApp/.../ui/v2/screens/settings/SecuritySettingsScreen.kt` | New | 2FA management in settings |
