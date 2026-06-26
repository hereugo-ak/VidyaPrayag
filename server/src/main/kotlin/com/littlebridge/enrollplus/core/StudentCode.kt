/*
 * File: StudentCode.kt
 * Module: core
 *
 * ISSUE 2a — ONE clean, predictable student_code standard.
 *
 * The codebase had accumulated at least seven mutually-incompatible formats
 * (DEMO-S001, S3-G2A-002, S-MQMRPM0R, STU-0020, S10A-1, C1, C3). None of them
 * could be derived from — or parsed back into — a student's class/section/roll,
 * so a code told you nothing and two schools could never share a convention.
 *
 * This object defines the single standard, derived directly from the data an
 * admin already enters when creating a student (class, section, roll):
 *
 *     <CLASS_TOKEN><SECTION>-<ROLL3>
 *
 *   • CLASS_TOKEN  a compact, stable token from the ClassNaming class key:
 *                  numeric classes  → "G" + number   (Grade 4  → "G4")
 *                  named classes    → up to 4 upper alnum  (Nursery → "NUR",
 *                                     LKG → "LKG")
 *   • SECTION      the canonical section letter (blank → "A")
 *   • ROLL3        the roll number; if purely numeric it is zero-padded to at
 *                  least 3 digits, otherwise upper-cased alnum is kept as-is.
 *
 *   Grade 4 / A / 7   → "G4A-007"
 *   10  / B / 12      → "G10B-012"
 *   Nursery / "" / 3  → "NURA-003"
 *
 * Codes are unique per school. When the derived code already exists, a numeric
 * "-2", "-3", … suffix is appended (collision handling) so generation never
 * fails and never silently overwrites another student.
 *
 * `generate` takes an `exists` probe so the caller controls the uniqueness
 * scope (a DB lookup) without this pure module depending on Exposed.
 */
package com.littlebridge.enrollplus.core

object StudentCode {

    /** Max characters kept for a NAMED (non-numeric) class token. */
    private const val NAMED_TOKEN_LEN = 4

    /**
     * Compact, stable class token for the code. Uses [ClassNaming.classKey] as
     * the canonical input so "Grade 4"/"class 4"/"IV" all map to "G4".
     */
    fun classToken(className: String?): String {
        val key = ClassNaming.classKey(className)        // "4", "10", "nursery", "lkg", ""
        if (key.isBlank()) return "X"
        // Pure number → "G" + number.
        if (key.all { it.isDigit() }) return "G$key"
        // Named class → up to NAMED_TOKEN_LEN upper-cased alnum.
        val alnum = key.uppercase().filter { it.isLetterOrDigit() }
        return alnum.take(NAMED_TOKEN_LEN).ifBlank { "X" }
    }

    /** Normalise the roll into the code's ROLL segment. */
    fun rollToken(rollNumber: String?): String {
        val raw = rollNumber?.trim().orEmpty()
        if (raw.isBlank()) return "000"
        val digits = raw.filter { it.isDigit() }
        return if (digits.isNotEmpty() && raw.all { it.isDigit() }) {
            // Pure numeric roll → zero-pad to 3.
            digits.trimStart('0').ifBlank { "0" }.padStart(3, '0')
        } else {
            // Mixed/alpha roll → keep upper alnum, capped for sanity.
            raw.uppercase().filter { it.isLetterOrDigit() }.take(6).ifBlank { "000" }
        }
    }

    /**
     * The canonical (pre-collision) student code for a (class, section, roll).
     * Deterministic and pure.
     */
    fun base(className: String?, section: String?, rollNumber: String?): String {
        val cls = classToken(className)
        val sec = ClassNaming.canonicalSection(section)   // blank → "A"
        val roll = rollToken(rollNumber)
        return "$cls$sec-$roll"
    }

    /**
     * Generate a UNIQUE student code for (class, section, roll). [exists] returns
     * true when a candidate is already taken in the desired scope (typically a
     * per-school DB lookup). Appends "-2", "-3", … on collision; bails to a
     * timestamp-suffixed fallback after a sane number of tries so it can never
     * loop forever.
     */
    fun generate(
        className: String?,
        section: String?,
        rollNumber: String?,
        exists: (String) -> Boolean,
    ): String {
        val base = base(className, section, rollNumber)
        if (!exists(base)) return base
        var n = 2
        while (n <= 999) {
            val candidate = "$base-$n"
            if (!exists(candidate)) return candidate
            n++
        }
        // Extremely unlikely fallback — still derived + unique-ish.
        return "$base-${System.currentTimeMillis().toString(36).uppercase()}"
    }
}
