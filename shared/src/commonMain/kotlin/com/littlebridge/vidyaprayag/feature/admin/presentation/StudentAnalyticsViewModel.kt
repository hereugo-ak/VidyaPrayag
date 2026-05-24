package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RiskStudent(
    val id: String,
    val name: String,
    val imageUrl: String,
    val retentionRisk: Int,
    val masteryTrend: String,
    val riskLevel: String // "Critical", "Medium", "Low"
)

data class SubjectEngagement(
    val name: String,
    val percentage: Float,
    val status: String? = null
)

data class StudentAnalyticsState(
    val dailyVolatility: List<Float> = listOf(0.92f, 0.88f, 0.94f, 0.91f, 0.98f, 0.89f, 0.95f, 0.93f, 0.90f, 0.64f, 0.85f, 0.89f, 0.91f),
    val criticalRiskCount: Int = 12,
    val mediumRiskCount: Int = 45,
    val lowRiskCount: Int = 782,
    val atRiskStudents: List<RiskStudent> = listOf(
        RiskStudent("1", "Julian Henderson", "https://lh3.googleusercontent.com/aida-public/AB6AXuCGBEHMfmZeOWRD7hmkKP6sVAOQPV6zUu9E7u21BU7VKWDUWq1k8uGkf36mbO3nkKkQAkSPrFFUCj7Nt-7a8U8YXluRw3Ja9xmGfkjD0fURnSq9dzOCudcBMFDC9q_7Qt2GEVnhovvnc7X2OHCbx90BstBNkWu0ykuWR5qSLsSXEapUlRr9C5gyIA5TOjuJfTKan6rsScqQuLHVR8eE-1SXxZ2A7QXU010GQn4XEEVKEC3zevaIH1DAToEhgKF9rXTwjuNs47eoYb5Z", 89, "-15% (1wk)", "Critical"),
        RiskStudent("2", "Marcus Sterling", "https://lh3.googleusercontent.com/aida-public/AB6AXuBe_PszObzkkoKHkUZgpntzEecc7yAvRf6Yss4uwd9yStT24uOdSck0TzWJFdIhsj8dY_ySayfmYl7vmKnCQKLJtIpwGhnBCUGPIVzetwAxn_Hi63DAyp-TL3Q4eMz032Rv7pUnCM8iXaDt30IVHO8NttQwxrEAeegQylZWxREDb7Lra30mspFIRjDEHF9aNq8jqinQshnDgFw-6R39ZEnSCqeJ8_cAvKIsxMyGTZYgYKHSkTOAVsCdVV9CXNqjkLGXgnbebWGRsmha", 62, "-8% (1wk)", "Medium")
    ),
    val subjectEngagements: List<SubjectEngagement> = listOf(
        SubjectEngagement("Advanced Mathematics", 0.942f),
        SubjectEngagement("Physical Sciences", 0.76f, "Risk Level High"),
        SubjectEngagement("Computer Programming", 0.918f)
    ),
    val cohortComparison: List<Float> = listOf(0.85f, 0.92f, 0.98f, 0.78f) // G9, G10, G11, G12
)

class StudentAnalyticsViewModel : ViewModel() {
    private val _state = MutableStateFlow(StudentAnalyticsState())
    val state: StateFlow<StudentAnalyticsState> = _state.asStateFlow()
}
