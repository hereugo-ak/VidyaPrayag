package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GalleryImage(
    val id: String,
    val url: String
)

data class InstitutionalProfileState(
    val schoolName: String = "St. Augustine Academy",
    val licenseType: String = "Global K-12 Institutional License",
    val location: String = "Metropolitan Education Zone, Block C",
    val profileImageUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuAPyD-N0QL-3lo77FwVM1B_6s2MHKtvg_v6sMqcU0_9oU3oNjr1iaTIwMjPyPwfpi-pI9XubjK8ZsKinKVCQ5Sy2JNbDU_p4kxIjIx7uAVpPEhcZb05GAN7puasE6rddIxPB9mdQZSHDwxz3_bRiTgxVH09vpB_A_goOB-rJgYjPD1yS9YYoguSB1az6YQpdF-dPRlO76Tl0c747nLB0fh3E1RRcMVY-nbVL1nEUDyYk0-n2-FgxfLM0t80W5I9FgSeFUFM9fnqUMtO",
    val isPublic: Boolean = true,
    val missionStatement: String = "",
    val learningModel: String = "Inquiry-Based Learning",
    val primaryLanguage: String = "English (International)",
    val activeTourName: String = "Main Campus - Innovation Wing",
    val galleryImages: List<GalleryImage> = listOf(
        GalleryImage("1", "https://lh3.googleusercontent.com/aida-public/AB6AXuBp5iDOVsT62ZuwVirmWfpsck9E_VsmLpY-0PNMIQoxyCkSGIRr_wWupRqerCyFKzZm5KTfGqhQL0Mi0-XSn6i9ZTdyU-B4fhcHqE-_0kjoe4hdJkuRf8WD_rXD0wmixhZh_eXcf7is7fWlM7ipssL51G_ceavQ30g4k2C0qyxcc9vUQO7zaxJw5r4XcyibQeRGP7Fp9nXT-sQBGOSRc9HIdPjWoLq27bkez--Yh_fuA9wyJ7neZGNg0GdHWzMa-jzFpdF2Bq-nDoYg"),
        GalleryImage("2", "https://lh3.googleusercontent.com/aida-public/AB6AXuDvIFJ7IG9UkYToJizhSCpDvc5HDcgFs7PE7aTs2QchL4aYcwDAAHeptHABzBYUTOUptExaLfLhXW1SIAYtk_T2gibVZQOdzgvy1YZaPbrUvcdyp3YZJwoLoU8y-Gd2Z9pbxPeK0xJ4URFULzNwB2SLAieQQ_ervzVdEs935UCiFSSfxPfS23nCv1dgTjiebyZ4mVvve_7Z2TXAh411WNA2dZDI0cEO_gHuMw-y1bLLq1yHyeoVk-khNI-Z3Oovv2mvaylAnWv5BAff"),
        GalleryImage("3", "https://lh3.googleusercontent.com/aida-public/AB6AXuDSpkyCEugViQIhswPOOlTNnYZ6fBPxSyD2j47GIkf6_DF5G6b3KihbDyoXVX4YO7fmNHQLYSSvmkyLiO_IMwGLxdasmeCMZoYNEfhY63rQn-ewQtNJlSyrjqWcra96KwM7VPAJNYG3rPQOQ91pjq0HloNuFDG-Rj4r8NGKMduy1kIE3Q5R8tWct7KUS4JG8h3khEafVIAyT30EAay-Wa3ZlUoNyV3ZXrbd_3l8HXXoUIK7guieOVV-8cTuanz-O7NjlCUBwe9fW-Mh")
    ),
    val storageUsage: Float = 0.48f,
    val profileCompletion: Int = 85
)

class InstitutionalProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(InstitutionalProfileState())
    val state: StateFlow<InstitutionalProfileState> = _state.asStateFlow()

    fun togglePublic(value: Boolean) {
        _state.value = _state.value.copy(isPublic = value)
    }

    fun updateMission(text: String) {
        _state.value = _state.value.copy(missionStatement = text)
    }

    fun updateLearningModel(model: String) {
        _state.value = _state.value.copy(learningModel = model)
    }

    fun updateLanguage(lang: String) {
        _state.value = _state.value.copy(primaryLanguage = lang)
    }
}
