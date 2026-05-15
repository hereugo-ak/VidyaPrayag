package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Enquiry(
    val id: String,
    val studentName: String,
    val parentName: String,
    val className: String,
    val date: String,
    val status: String, // "New", "Follow-up", "Converted", "Dropped"
    val parentContact: String
)

data class AdmissionCRMState(
    val totalEnquiries: Int = 124,
    val newEnquiries: Int = 12,
    val followUps: Int = 45,
    val conversions: Int = 28,
    val conversionRate: Float = 22.5f,
    val recentEnquiries: List<Enquiry> = listOf(
        Enquiry("1", "Aryan Sharma", "Rajesh Sharma", "Class 8", "12 May, 2024", "New", "+91 98765 43210"),
        Enquiry("2", "Isha Patel", "Meena Patel", "Nursery", "11 May, 2024", "Follow-up", "+91 98765 43211"),
        Enquiry("3", "Vihaan Gupta", "Sanjay Gupta", "Class 2", "10 May, 2024", "Converted", "+91 98765 43212")
    )
)

class AdmissionCRMViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdmissionCRMState())
    val state: StateFlow<AdmissionCRMState> = _state.asStateFlow()

    fun updateEnquiryStatus(enquiryId: String, newStatus: String) {
        val updated = _state.value.recentEnquiries.map {
            if (it.id == enquiryId) it.copy(status = newStatus) else it
        }
        _state.value = _state.value.copy(recentEnquiries = updated)
    }
}
