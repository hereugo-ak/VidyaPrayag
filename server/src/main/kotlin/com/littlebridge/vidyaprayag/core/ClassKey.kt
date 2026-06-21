/*
 * File: ClassKey.kt
 * Module: core
 *
 * ROOT FIX (ISSUE 1 — teacher⇄student⇄subject⇄class relationships never connect).
 *
 * The whole relationship chain on this platform is DERIVED, not stored: a teacher
 * is connected to a student because the teacher has a row in
 * `teacher_subject_assignments` whose (class_name, section) equals the student's
 * (class_name, section) in `students`. There is no foreign key — the join is a
 * plain string equality.
 *
 * That join was being done with raw, exact `eq` comparisons on FREE-TEXT columns
 * that different code paths populate from different sources:
 *   • students.class_name / section  ← whatever an admin typed in the add-student
 *     dialog ("Grade 4", "grade 4 ", "4", "Class 4", "IV", section "a"/"A"/"")
 *   • teacher_subject_assignments.class_name / section ← the canonical
 *     school_classes.name (e.g. "Grade 4") or a free-text request value.
 *
 * Any difference in case, surrounding/inner whitespace, or the "Grade/Class/Std"
 * prefix made the equality silently fail, so the student connected to ZERO
 * teachers even though the admin had assigned them — the exact "nothing surfaces
 * anywhere" symptom in the report.
 *
 * This object is the single source of truth for turning a human class label +
 * section into a stable, comparable KEY. It is used at every WRITE (so stored
 * values are already canonical) and every JOIN/READ (so two values that mean the
 * same class always match). Centralising it here means the chain can never drift
 * again from one route to the next.
 */
package com.littlebridge.vidyaprayag.core

/**
 * A normalised, comparable identity for a (class, section) pair. Two labels that
 * a human would consider the same class+section always produce the SAME
 * ClassKey, regardless of case / spacing / "Grade|Class|Std" prefixing.
 */
data class ClassKey(val classKey: String, val sectionKey: String) {
    /** A single stable string, handy for map keys / grouping. */
    val composite: String get() = "$classKey|$sectionKey"
}

object ClassNaming {

    /** Prefix words we strip when deriving the comparable class token. */
    private val CLASS_PREFIXES = listOf("grade", "class", "standard", "std", "std.", "cls")

    /**
     * Roman-numeral → arabic map so "Grade IV", "Class 4" and "4" all collapse to
     * the same key. Covers 1..12 (the realistic K-12 range).
     */
    private val ROMAN = mapOf(
        "i" to "1", "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10", "xi" to "11", "xii" to "12"
    )

    /** Collapse all runs of whitespace to a single space and trim the ends. */
    private fun collapseSpaces(s: String): String =
        s.trim().replace(Regex("\\s+"), " ")

    /**
     * Canonical COMPARISON key for a class label. Lower-cased, prefix-stripped,
     * roman-normalised, whitespace-collapsed. NOT for display — use the stored
     * canonical name for that. Pure (no DB).
     *
     *   "Grade 4"  → "4"      "grade  4 " → "4"     "Class IV" → "4"
     *   "Nursery"  → "nursery"  "LKG" → "lkg"      "10" → "10"
     */
    fun classKey(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = collapseSpaces(raw).lowercase()
        // Strip a leading prefix word (and any separator after it).
        for (p in CLASS_PREFIXES) {
            if (s == p) { s = ""; break }
            if (s.startsWith("$p ")) { s = s.removePrefix("$p ").trim(); break }
            if (s.startsWith("$p-")) { s = s.removePrefix("$p-").trim(); break }
        }
        // Roman → arabic when the remainder is a pure roman numeral.
        ROMAN[s]?.let { return it }
        // Drop incidental separators so "g-4" / "g 4" don't fork from "g4".
        return s.replace(Regex("\\s+"), "")
    }

    /**
     * Canonical COMPARISON key for a section. Upper-cased + trimmed; blank
     * defaults to "A" (the project-wide section default). "a" / " A " → "A".
     */
    fun sectionKey(raw: String?): String {
        val s = raw?.let { collapseSpaces(it) }?.uppercase().orEmpty()
        return s.ifBlank { "A" }
    }

    /** Build the comparable [ClassKey] for a (class, section) pair. */
    fun key(className: String?, section: String?): ClassKey =
        ClassKey(classKey(className), sectionKey(section))

    /** True when two (class, section) pairs refer to the same class+section. */
    fun sameClassSection(
        classA: String?, sectionA: String?,
        classB: String?, sectionB: String?,
    ): Boolean = key(classA, sectionA) == key(classB, sectionB)

    /**
     * The canonical DISPLAY/STORAGE section: trimmed + upper-cased, blank → "A".
     * (Sections are always stored in canonical form so the equality holds without
     * needing the key at read time, but the key is still used defensively.)
     */
    fun canonicalSection(raw: String?): String = sectionKey(raw)
}
