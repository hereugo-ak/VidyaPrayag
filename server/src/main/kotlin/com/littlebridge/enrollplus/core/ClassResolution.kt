/*
 * File: ClassResolution.kt
 * Module: core
 *
 * ROOT FIX (ISSUE 1) — write-side half of the class normalisation.
 *
 * [ClassNaming] gives us a comparable KEY for a class label. This object uses
 * that key to resolve a free-text class label an admin typed back to the
 * school's CANONICAL `school_classes.name`, so the value we persist in
 * `students.class_name` is byte-for-byte identical to the value stored in
 * `teacher_subject_assignments.class_name` (which is itself the canonical
 * school_classes.name). With both ends storing the canonical string, the derived
 * teacher⇄student join holds with a plain equality and never silently drops.
 *
 * If a typed class doesn't resolve to a configured class (e.g. the school never
 * created it), we fall back to a cleaned-up version of what was typed
 * (whitespace-collapsed) so nothing is rejected outright — but a configured
 * class always wins, which is what makes the relationship chain connect.
 */
package com.littlebridge.enrollplus.core

import com.littlebridge.enrollplus.db.SchoolClassesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

object ClassResolution {

    /**
     * Resolve [typedClassName] to the school's canonical class name. Must run
     * inside an Exposed transaction (call from `dbQuery { ... }`).
     *
     * Resolution order (first hit wins):
     *   1. canonical class whose NAME matches by [ClassNaming.classKey]
     *   2. canonical class whose CODE matches by [ClassNaming.classKey]
     *   3. the cleaned-up typed value (whitespace-collapsed, original casing kept)
     */
    fun canonicalClassName(schoolId: UUID, typedClassName: String): String {
        val cleaned = typedClassName.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return cleaned
        val wantKey = ClassNaming.classKey(cleaned)

        val rows = SchoolClassesTable.selectAll()
            .where { SchoolClassesTable.schoolId eq schoolId }
            .toList()

        // 1) match on canonical name
        rows.firstOrNull { ClassNaming.classKey(it[SchoolClassesTable.name]) == wantKey }
            ?.let { return it[SchoolClassesTable.name] }
        // 2) match on class code
        rows.firstOrNull { ClassNaming.classKey(it[SchoolClassesTable.code]) == wantKey }
            ?.let { return it[SchoolClassesTable.name] }
        // 3) honest fallback — keep the cleaned typed value
        return cleaned
    }
}
