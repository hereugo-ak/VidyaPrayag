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

data class TutorChatState(
    val isLoading: Boolean = false,
    val question: String = "",
    val turn: TutorTurnDto? = null,
    val conversationHistory: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val subjectId: String = "",
    val subjects: List<SubjectItemDto> = emptyList(),
    val isLoadingSubjects: Boolean = false,
)

data class ChatMessage(
    val role: String,        // "user" | "tutor"
    val text: String,
    val nextPrompt: String? = null,
    val isPractice: Boolean = false,
    val practiceQuestions: List<PracticeQuestionDto>? = null,
)

class TutorChatViewModel(
    private val repository: TutorRepository,
    private val preferenceRepository: PreferenceRepository,
    private val selectedChildHolder: SelectedChildHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(TutorChatState())
    val state: StateFlow<TutorChatState> = _state.asStateFlow()

    fun updateQuestion(text: String) {
        _state.update { it.copy(question = text) }
    }

    fun updateSubject(subjectId: String) {
        _state.update { it.copy(subjectId = subjectId) }
    }

    fun loadSubjects() {
        val childId = selectedChildHolder.selectedChildId.value ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingSubjects = true) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoadingSubjects = false) }
                return@launch
            }
            when (val result = repository.getSubjects(token, childId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoadingSubjects = false,
                            subjects = result.data.data ?: emptyList(),
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoadingSubjects = false) }
                }
            }
        }
    }

    fun askDoubt() {
        val current = _state.value
        if (current.question.isBlank()) return

        val childId = selectedChildHolder.selectedChildId.value ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    conversationHistory = it.conversationHistory + ChatMessage(
                        role = "user",
                        text = current.question,
                    ),
                )
            }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.askDoubt(token, DoubtRequest(childId, current.subjectId, current.question))) {
                is NetworkResult.Success -> {
                    val resultDto = result.data.data
                    if (resultDto != null) {
                        val turn = resultDto.turn
                        _state.update {
                            it.copy(
                                isLoading = false,
                                turn = turn,
                                question = "",
                                conversationHistory = it.conversationHistory + ChatMessage(
                                    role = "tutor",
                                    text = turn.studentFacing.text,
                                    nextPrompt = turn.studentFacing.nextPrompt,
                                    isPractice = turn.practice?.isNotEmpty() == true,
                                    practiceQuestions = turn.practice,
                                ),
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = result.data.message) }
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(isLoading = false, error = "Connection error") }
                }
            }
        }
    }

    fun clearConversation() {
        _state.update { it.copy(conversationHistory = emptyList(), turn = null, error = null) }
    }
}
