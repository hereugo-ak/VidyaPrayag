package com.littlebridge.enrollplus.feature.tutor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.core.state.SelectedChildHolder
import com.littlebridge.enrollplus.feature.tutor.domain.model.*
import com.littlebridge.enrollplus.feature.tutor.domain.repository.TutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TutorPracticeState(
    val isLoading: Boolean = false,
    val isGrading: Boolean = false,
    val currentQuestion: PracticeQuestionDto? = null,
    val selectedAnswer: String = "",
    val gradeResult: PracticeGradeDto? = null,
    val subjectId: String = "",
    val error: String? = null,
)

class TutorPracticeViewModel(
    private val repository: TutorRepository,
    private val preferenceRepository: PreferenceRepository,
    private val selectedChildHolder: SelectedChildHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(TutorPracticeState())
    val state: StateFlow<TutorPracticeState> = _state.asStateFlow()

    fun updateSubject(subjectId: String) {
        _state.update { it.copy(subjectId = subjectId) }
    }

    fun setQuestion(question: PracticeQuestionDto) {
        _state.update {
            it.copy(
                currentQuestion = question,
                selectedAnswer = "",
                gradeResult = null,
            )
        }
    }

    fun selectAnswer(answer: String) {
        _state.update { it.copy(selectedAnswer = answer) }
    }

    fun submitAnswer() {
        val current = _state.value
        val question = current.currentQuestion ?: return
        val childId = selectedChildHolder.selectedChildId.value ?: return
        val subjectId = current.subjectId.ifBlank { return }

        viewModelScope.launch {
            _state.update { it.copy(isGrading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isGrading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.gradePractice(
                token,
                PracticeGradeRequest(
                    childId = childId,
                    subjectId = subjectId,
                    questionId = question.questionId,
                    stem = question.stem,
                    options = question.options,
                    answerKey = question.answerKey,
                    topicId = question.topicId,
                    difficulty = question.difficulty,
                    childAnswer = current.selectedAnswer,
                ),
            )) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isGrading = false,
                            gradeResult = result.data.data,
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isGrading = false, error = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(isGrading = false, error = "Connection error") }
                }
            }
        }
    }

    fun nextQuestion() {
        _state.update {
            it.copy(
                currentQuestion = null,
                selectedAnswer = "",
                gradeResult = null,
            )
        }
    }
}
