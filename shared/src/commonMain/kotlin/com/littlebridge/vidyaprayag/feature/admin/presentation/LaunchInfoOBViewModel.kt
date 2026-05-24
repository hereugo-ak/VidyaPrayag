package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComplianceDocument(
    val id: String,
    val name: String,
    val status: String, // "Uploaded", "Awaiting"
    val metadata: String? = null
)

data class AppModule(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val iconName: String
)

data class LaunchInfoState(
    val schoolName: String = "St. Augustine Academy",
    val licenseType: String = "Global K-12 Institutional License",
    val location: String = "Metropolitan Education Zone, Block C",
    val imageUrl: String = "https://lh3.googleusercontent.com/aida/ADBb0uja34Re_-MtOF9jh5ZyVhQGKS4GfxPzJYtBhBlW10Xem3awSStEWcQapUQMn84PxpJewsaPADpJFUHEmmurRCYaMQxn0RrEMUfKnhgm5x3e5L9NVqRF2PYk3JLfBHm3wWG-9FO94L6Jfs9G9hvcp3m8H9AaL9HhsNrARYaA6ptaWgvQCqXhGxbZi53-E2MLeaH0zRuQxWq_uOFhJXfrZhZ3jYiOErFrXZwdVHYDZTVj-ULoIjrXMisgtdSn",
    val documents: List<ComplianceDocument> = listOf(
        ComplianceDocument("1", "Affiliation Certificate", "Uploaded", "PDF • 2.4 MB • Uploaded May 12"),
        ComplianceDocument("2", "RTE Compliance", "Awaiting")
    ),
    val modules: List<AppModule> = listOf(
        AppModule("1", "Student LMS", "Core Learning Experience", true, "school"),
        AppModule("2", "Fee Management", "Automated Financial Flows", true, "payments"),
        AppModule("3", "Parent Gateway", "Real-time Communication", false, "forum")
    )
)

class LaunchInfoOBViewModel : ViewModel() {
    private val _state = MutableStateFlow(LaunchInfoState())
    val state: StateFlow<LaunchInfoState> = _state.asStateFlow()

    fun toggleModule(moduleId: String) {
        val updatedModules = _state.value.modules.map {
            if (it.id == moduleId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _state.value = _state.value.copy(modules = updatedModules)
    }
}
