package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StudentResult(
    val id: String,
    val name: String,
    val imageUrl: String,
    val attendance: String,
    val score: String,
    val status: String, // "Exceeding", "Pending", "Meeting"
    val trend: String // e.g. "+2.4%"
)

data class ResultsState(
    val selectedTest: String = "Unit Test II",
    val selectedClass: String = "Grade 10-A",
    val selectedSubject: String = "Chemistry",
    val classAverage: String = "78.4",
    val averageTrend: String = "+4.2%",
    val exceedingCount: Int = 12,
    val meetingCount: Int = 18,
    val belowCount: Int = 4,
    val students: List<StudentResult> = listOf(
        StudentResult("ED-001", "Marcus Holloway", "https://lh3.googleusercontent.com/aida-public/AB6AXuDpV-4PKT_XbHWI3uAvy9zI3DLC-vIGAN8nH5hRU70ojOugaGAoW5o4wcG2SxFlyArLBMqKH6sl6KGsIdJpYef0iWymRj2DOgWoZK_MuBz8-n-GtwBK82ODDHPaC5UkhXGDCeY4nx7JawEdrkKQfi2QrlQfH3LaSme4LyeQ4BcjUwZfCxmtT1kUvSkbzhiD7w5TdNGdCsExiD_miH5t-OP9Vx9e0xn39g6a75RT90EtbU5-tAyk8TWjeNxaV0-uBylraSTSJY1dP37F", "100%", "98", "Exceeding", "+2.4%"),
        StudentResult("ED-002", "Elena Rodriguez", "https://lh3.googleusercontent.com/aida-public/AB6AXuDmFss_L9TaoDnR-KEQ3Gu_ast0ZZKv-d_Qc3WkK84_98PKcXriYFtwnNT9Pv8mZzO-fwJ6qEkLQlY3IPLiz-brFV4WSLqo2ia-oCwk5gTJexksEIb68cRAYnDcsW08OhBLd933e3QmcO8bFvwUr01spATd7vc4IIiItQkxOgi2UFef0h9j09wugOIqzmQ4jMGG8dpMbIeLgoG91U5BtuHhU-hXVdaHkWbpR_BumXbfubDnCzs21Ri0dzDLsOppHWhYTDM4TUNb6StF", "96%", "", "Pending", "0%"),
        StudentResult("ED-003", "Julian Chen", "https://lh3.googleusercontent.com/aida-public/AB6AXuBii3SEPqItbl332NM7R4H_UVF6p6VvNBdUFFAOv2zfqMqBq6fgEcmHDESfOiSfuXTnAYhUFGpBPEaMFpxjVZEbzN_yE3EFapxZlyE24DW9ja_WxKr8TEm18vq7JIfxCJDazrcYa8Ia9YmN4oK1a3tJzygHIoTCEM749-OznockMm0ezcSbznbrRJ8HN6uKhM7AmNdmJl3gdKhq_uelF5nOFCSsBE5lG5wK1KdvPTDa1B1mrhYihQQaa56nhh2z8IWEIvExjhOA3976", "82%", "74", "Meeting", "-1.2%")
    )
)

class ResultsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    fun selectTest(test: String) {
        _state.value = _state.value.copy(selectedTest = test)
    }

    fun selectClass(className: String) {
        _state.value = _state.value.copy(selectedClass = className)
    }

    fun selectSubject(subject: String) {
        _state.value = _state.value.copy(selectedSubject = subject)
    }
}
