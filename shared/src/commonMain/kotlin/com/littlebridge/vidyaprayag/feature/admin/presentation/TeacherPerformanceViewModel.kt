package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StarTeacher(
    val rank: Int,
    val name: String,
    val department: String,
    val score: Double,
    val imageUrl: String
)

data class FacultyAccountability(
    val id: String,
    val name: String,
    val department: String,
    val complianceScore: Int,
    val avgUpdateDelay: String,
    val studentAvgMark: String,
    val riskCorrelation: String, // "Stable", "High Risk", "Watching"
    val initials: String
)

data class DeptEfficiency(
    val name: String,
    val percentage: Int
)

data class TeacherPerformanceState(
    val aggregateCompliance: String = "94.2%",
    val complianceTrend: String = "+2.4% from last month",
    val syllabusUpdateTrend: List<Float> = listOf(0.4f, 0.6f, 0.5f, 0.85f, 0.7f, 1.0f, 0.5f, 0.75f),
    val starFaculty: List<StarTeacher> = listOf(
        StarTeacher(1, "Dr. Sarah Jenkins", "Mathematics", 99.8, "https://lh3.googleusercontent.com/aida-public/AB6AXuC4bGKo5RDKEDVsqEANNGgLXgIlytdpqdrdA_0ZgHuCQGXpXdtFHxG3o7NqqVJ2omzhFKSILPKQ7zrevPEfbnZrtnqv9oMjaTvV6zQzlVckP-xwUNiRk1_cW8yHIK9-bnN1fvx4ZMaigbyh5AUwLsYzOfZ7xmkV15fr7Be-5nBa-7DKkZp5w5phY_k9KE40NjIFiULHjjEsVQfz7umnAyrGU_SsRtB7EeCeC8I0D_smfiUfWmSZSstZJ1NCShIzXAzOTFRIqS-JI5YQ"),
        StarTeacher(2, "Prof. Michael Chen", "Advanced Physics", 98.2, "https://lh3.googleusercontent.com/aida-public/AB6AXuAmb6zEpF5u9hV-p8tcioMul2YEOpXplnJzyO4v94TGPz7O5-rMyRWrc8DigbRApilTWxPxBpUs2NpGbY_XOS7G0KPrdxixuMU9tvfPeTqt9K5j7ur7-aJp6gPMvAqfTjlUOyME8dLfobMYSNIh4-Ql9nt7yKq9-GqjfAlyOLf0QPFzUzdX0AV_UcJQvSy1H3jFFvqJr-E-q8O31aRJLqRL3AqkDo3nvr0wXF9GCFcHJN9Cq-My4KBJKlvKKrhHlC8mtLNiMXygdQzL"),
        StarTeacher(3, "Elena Rodriguez", "History", 97.5, "https://lh3.googleusercontent.com/aida-public/AB6AXuANstZzXeW5n85JbsAoy4FuQwl7oBhuluVyIFBXrmva3zbK6IW0OaE_aqbz21DoKHhJzCuscLVcdXXjycLVdktBY1SZ-Ex8m_8KvjCpdPEwZzXFUyX5Awc97KWg9uRwxI89lvhkEZZFFC6N1wLILdRxZM3fdA5hmCMF-2jVjmPWIKGxZmvCytwz--hYKh99l9nNfQuM8o5TuqnrBXClgF-xDIInr4WLMDBni_FQXyB3ehMlpUtomOwU5kigI5AWN5dlHeEfUDDcODAV")
    ),
    val accountabilityMatrix: List<FacultyAccountability> = listOf(
        FacultyAccountability("1", "James Miller", "Chemistry Dept.", 92, "1.2 Days", "84.5%", "Stable", "JM"),
        FacultyAccountability("2", "Bradley Thompson", "Literature Dept.", 68, "5.8 Days", "71.2%", "High Risk", "BT"),
        FacultyAccountability("3", "Linda Wright", "Sociology Dept.", 81, "2.5 Days", "78.0%", "Watching", "LW")
    ),
    val deptEfficiencies: List<DeptEfficiency> = listOf(
        DeptEfficiency("Science & Technology", 96),
        DeptEfficiency("Humanities", 84),
        DeptEfficiency("Physical Education", 92)
    )
)

class TeacherPerformanceViewModel : ViewModel() {
    private val _state = MutableStateFlow(TeacherPerformanceState())
    val state: StateFlow<TeacherPerformanceState> = _state.asStateFlow()
}
