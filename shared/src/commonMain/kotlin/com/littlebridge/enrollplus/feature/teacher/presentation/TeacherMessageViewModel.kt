package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherMessageDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherMessageThreadDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherSendMessageRequest
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TeacherMessageState(
    val threads: List<TeacherMessageThreadDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val openThreadId: String? = null,
    val openThreadName: String = "",
    val messages: List<TeacherMessageDto> = emptyList(),
    val conversationLoading: Boolean = false,
    val conversationError: String? = null,
    val sending: Boolean = false,
    val replyError: String? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && threads.isEmpty()
    val conversationEmpty: Boolean
        get() = !conversationLoading && conversationError == null && messages.isEmpty()
}

class TeacherMessageViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }

    private val _state = MutableStateFlow(TeacherMessageState())
    val state: StateFlow<TeacherMessageState> = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var pollJob: Job? = null

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    fun loadThreads() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val token = token() ?: run {
                _state.update { it.copy(loading = false, error = "Not signed in") }
                return@launch
            }
            when (val r = repository.getMessageThreads(token)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(loading = false, threads = r.data.data.threads) }
                is NetworkResult.Error ->
                    _state.update { it.copy(loading = false, error = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(loading = false, error = "Connection error") }
            }
        }
    }

    fun openThread(threadId: String, fallbackName: String) {
        stopPolling()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    openThreadId = threadId,
                    openThreadName = fallbackName,
                    messages = emptyList(),
                    conversationLoading = true,
                    conversationError = null,
                    replyError = null,
                )
            }
            val token = token() ?: run {
                _state.update { it.copy(conversationLoading = false, conversationError = "Not signed in") }
                return@launch
            }
            when (val r = repository.getThreadMessages(token, threadId)) {
                is NetworkResult.Success -> {
                    val data = r.data.data
                    _state.update {
                        it.copy(
                            conversationLoading = false,
                            messages = data?.messages ?: emptyList(),
                            openThreadName = data?.senderName ?: fallbackName,
                        )
                    }
                    loadThreads()
                    startPolling()
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(conversationLoading = false, conversationError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(conversationLoading = false, conversationError = "Connection error") }
            }
        }
    }

    fun closeThread() {
        stopPolling()
        _state.update {
            it.copy(openThreadId = null, openThreadName = "", messages = emptyList(), conversationError = null, replyError = null)
        }
    }

    fun markAsRead(threadId: String) {
        val target = _state.value.threads.firstOrNull { it.id == threadId } ?: return
        if (target.isRead && target.unreadCount == 0) return

        _state.update {
            it.copy(threads = it.threads.map { thread ->
                if (thread.id == threadId) thread.copy(isRead = true, unreadCount = 0) else thread
            })
        }

        viewModelScope.launch {
            val token = token() ?: return@launch
            when (repository.markThreadRead(token, threadId)) {
                is NetworkResult.Success -> {
                    if (_state.value.openThreadId == threadId) {
                        reloadConversation()
                    }
                    refreshUnreadCount(token)
                }
                else -> loadThreads()
            }
        }
    }

    private fun reloadConversation() {
        val threadId = _state.value.openThreadId ?: return
        viewModelScope.launch {
            val token = token() ?: return@launch
            when (val r = repository.getThreadMessages(token, threadId)) {
                is NetworkResult.Success -> {
                    val data = r.data.data
                    _state.update {
                        it.copy(
                            messages = data?.messages ?: emptyList(),
                            openThreadName = data?.senderName ?: it.openThreadName,
                            conversationError = null,
                        )
                    }
                    loadThreads()
                }
                is NetworkResult.Error -> {
                    if (_state.value.conversationError == null) {
                        _state.update { it.copy(conversationError = r.message) }
                    }
                }
                is NetworkResult.ConnectionError -> {
                    if (_state.value.conversationError == null) {
                        _state.update { it.copy(conversationError = "Connection error") }
                    }
                }
            }
        }
    }

    fun reply(body: String) {
        val threadId = _state.value.openThreadId ?: return
        if (body.isBlank()) return

        val tempId = "optimistic-${kotlin.random.Random.nextLong()}"
        val optimisticMsg = TeacherMessageDto(
            id = tempId,
            body = body.trim(),
            isMine = true,
            createdAt = "",
            time = "Now",
            status = "SENT",
        )
        _state.update {
            it.copy(
                messages = it.messages + optimisticMsg,
                sending = true,
                replyError = null,
            )
        }

        viewModelScope.launch {
            val token = token() ?: run {
                _state.update {
                    it.copy(
                        messages = it.messages.filterNot { msg -> msg.id == tempId },
                        sending = false,
                        replyError = "Not signed in",
                    )
                }
                return@launch
            }
            val req = TeacherSendMessageRequest(
                threadId = threadId,
                body = body.trim(),
                clientMsgId = kotlin.random.Random.nextLong().toString(),
            )
            when (val r = repository.sendMessage(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(sending = false, replyError = null) }
                    reloadConversation()
                }
                is NetworkResult.Error ->
                    _state.update {
                        it.copy(
                            messages = it.messages.filterNot { msg -> msg.id == tempId },
                            sending = false,
                            replyError = r.message,
                        )
                    }
                is NetworkResult.ConnectionError ->
                    _state.update {
                        it.copy(
                            messages = it.messages.filterNot { msg -> msg.id == tempId },
                            sending = false,
                            replyError = "Connection error",
                        )
                    }
            }
        }
    }

    fun clearReplyError() {
        _state.update { it.copy(replyError = null) }
    }

    private fun refreshUnreadCount(token: String) {
        viewModelScope.launch {
            when (val r = repository.getUnreadCount(token)) {
                is NetworkResult.Success -> _unreadCount.value = r.data
                else -> {}
            }
        }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_state.value.openThreadId != null && !_state.value.sending) {
                    reloadConversation()
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
