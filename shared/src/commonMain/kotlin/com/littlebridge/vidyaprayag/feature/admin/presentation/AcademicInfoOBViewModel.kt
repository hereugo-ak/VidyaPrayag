package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Subject(
    val id: String,
    val name: String,
    val teacherName: String? = null,
    val teacherImageUrl: String? = null,
    val iconName: String
)

data class AcademicInfoState(
    val selectedClass: String = "Class 8",
    val availableClasses: List<String> = listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6"),
    val subjects: List<Subject> = listOf(
        Subject("1", "Mathematics", "Dr. Arpita Sharma", "https://lh3.googleusercontent.com/aida/ADBb0uja34Re_-MtOF9jh5ZyVhQGKS4GfxPzJYtBhBlW10Xem3awSStEWcQapUQMn84PxpJewsaPADpJFUHEmmurRCYaMQxn0RrEMUfKnhgm5x3e5L9NVqRF2PYk3JLfBHm3wWG-9FO94L6Jfs9G9hvcp3m8H9AaL9HhsNrARYaA6ptaWgvQCqXhGxbZi53-E2MLeaH0zRuQxWq_uOFhJXfrZhZ3jYiOErFrXZwdVHYDZTVj-ULoIjrXMisgtdSn", "functions"),
        Subject("2", "Science", null, null, "science"),
        Subject("3", "History", "Prof. Julian V.", "https://lh3.googleusercontent.com/aida/ADBb0uiWZq-wW7_m3lnCFUGBlp8nNDYWY21_qf3JGIcMi0WAVHrgz3sKvAVEqHkh4GjXzxnbWzM4REDpGq0Obs8OTeRwFiZwYD2Td72VDGFa_AB4Gb5ovj3qVT23q4RIEIwUNiL206dYGFVg-00XLz1ZW9Snf_5oITRCj3wQLxRCx9Mklbb72D87jxf1Ocgjljni7TJj2JBTkDQXYj-6U9mmfMVIuGuIQ0s_oFjGSdyExIcf3jwNjwV46aONy6TZ", "history_edu")
    ),
    val syncActive: Boolean = true,
    val curriculumPrecision: Int = 92
)

class AcademicInfoOBViewModel : ViewModel() {
    private val _state = MutableStateFlow(AcademicInfoState())
    val state: StateFlow<AcademicInfoState> = _state.asStateFlow()

    fun selectClass(className: String) {
        _state.value = _state.value.copy(selectedClass = className)
    }
}
