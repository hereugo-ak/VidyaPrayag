/*
 * File: PhoneNormalizer.kt
 * Module: core
 *
 * ISSUE 2b / 2d — a single, shared phone-number normaliser so the number an
 * admin types when CREATING a student and the number a parent types when
 * LINKING a child compare equal even if formatted differently
 * ("+91 98765 43210", "098765-43210", "9876543210").
 *
 * Strategy: keep only digits, drop a single leading country/zero prefix so the
 * meaningful subscriber number is compared. We keep the LAST 10 digits as the
 * comparison key (the Indian mobile length the platform targets) while still
 * validating that the full input is plausible. Pure (no dependencies) so it can
 * be reused on every layer.
 */
package com.littlebridge.vidyaprayag.core

object PhoneNormalizer {

    /** Strip everything except digits. */
    fun digitsOnly(raw: String?): String = raw?.filter { it.isDigit() }.orEmpty()

    /**
     * The canonical COMPARISON key for a phone number: its last 10 digits.
     * Returns "" when there aren't enough digits to be a real number.
     */
    fun key(raw: String?): String {
        val d = digitsOnly(raw)
        return if (d.length >= 10) d.takeLast(10) else ""
    }

    /**
     * Canonical STORAGE form: the 10-digit subscriber number when we have one,
     * otherwise the trimmed digits (so we never silently lose a partial value).
     */
    fun canonical(raw: String?): String {
        val d = digitsOnly(raw)
        return if (d.length >= 10) d.takeLast(10) else d
    }

    /** A plausible mobile number (>= 10 meaningful digits). */
    fun isValid(raw: String?): Boolean = key(raw).isNotEmpty()

    /** True when two phone inputs refer to the same subscriber number. */
    fun sameNumber(a: String?, b: String?): Boolean {
        val ka = key(a); val kb = key(b)
        return ka.isNotEmpty() && ka == kb
    }
}
