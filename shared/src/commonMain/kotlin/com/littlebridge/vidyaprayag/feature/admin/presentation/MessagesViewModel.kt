/*
 * File: MessagesViewModel.kt
 * Module: feature.admin.presentation
 *
 * Drives the admin Messages screen. On init we hit
 *   GET /api/v1/school/messages/threads
 * and expose:
 *   - [state] : threads list + transient flags (sending)
 *   - [isLoading], [errorMessage] : standard loading state
 *   - [refresh]            : explicit re-pull
 *   - [markAsRead]         : optimistic patch + POST /threads/{id}/read
 *   - [sendMessage]        : POST /messages, then refresh()
 *
 * Server contract: server/.../feature/school/MessagesRouting.kt
 */
package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MessageThread
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.MessagesRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for the messages list.
 *
 * `isSending` is a separate flag from `isLoading` because the user can hit
 * "Send" while a refresh is in flight (and vice-versa); collapsing them into
 * one flag would force the inbox into a skeleton state every time you send a
 * line, which is the wrong UX.
 */
data class MessagesState(
    val threads: List<MessageThread> = emptyList(),
    val isSending: Boolean = false
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesState())
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("MessagesVM", "No auth token in prefs; skipping refresh")
                _isLoading.value = false
                return@launch
            }

            when (val result = messagesRepository.getThreads(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(threads = result.data)
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    AppLogger.e("MessagesVM", "getThreads failed: ${result.message}")
                }
                is NetworkResult.ConnectionError -> {
                    _errorMessage.value = "Connection error"
                    AppLogger.e("MessagesVM", "getThreads connection error")
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Optimistically marks the local thread as read, then fires the server
     * call. On failure we revert the optimistic edit and surface the error.
     */
    fun markAsRead(threadId: String) {
        val previous = _state.value
        val target = previous.threads.firstOrNull { it.id == threadId } ?: return
        if (target.isRead && target.unreadCount == 0) return // already read - no-op

        _state.value = previous.copy(
            threads = previous.threads.map {
                if (it.id == threadId) it.copy(isRead = true, unreadCount = 0) else it
            }
        )

        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("MessagesVM", "No auth token in prefs; reverting markAsRead")
                _state.value = previous
                _errorMessage.value = "Not signed in"
                return@launch
            }

            when (val result = messagesRepository.markThreadRead(token, threadId)) {
                is NetworkResult.Success -> {
                    AppLogger.d("MessagesVM", "Marked thread $threadId as read")
                }
                is NetworkResult.Error -> {
                    AppLogger.e("MessagesVM", "markThreadRead failed: ${result.message}")
                    _state.value = previous
                    _errorMessage.value = result.message
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("MessagesVM", "markThreadRead connection error")
                    _state.value = previous
                    _errorMessage.value = "Connection error"
                }
            }
        }
    }

    /**
     * Sends a message (optionally appending to an existing thread). On success
     * we re-pull threads to pick up the server-formatted preview and time.
     *
     * The caller can pass a callback that fires only on success - useful for
     * dismissing the compose sheet / clearing the input field.
     */
    fun sendMessage(
        body: String,
        threadId: String? = null,
        onSent: (() -> Unit)? = null
    ) {
        if (body.isBlank()) {
            _errorMessage.value = "Message body cannot be empty"
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("MessagesVM", "No auth token in prefs; aborting send")
                _state.value = _state.value.copy(isSending = false)
                _errorMessage.value = "Not signed in"
                return@launch
            }

            val request = SendMessageRequest(threadId = threadId, body = body)
            when (val result = messagesRepository.sendMessage(token, request)) {
                is NetworkResult.Success -> {
                    AppLogger.d(
                        "MessagesVM",
                        "Sent message ${result.data.messageId} (thread ${result.data.threadId})"
                    )
                    _state.value = _state.value.copy(isSending = false)
                    onSent?.invoke()
                    refresh()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("MessagesVM", "sendMessage failed: ${result.message}")
                    _state.value = _state.value.copy(isSending = false)
                    _errorMessage.value = result.message
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("MessagesVM", "sendMessage connection error")
                    _state.value = _state.value.copy(isSending = false)
                    _errorMessage.value = "Connection error"
                }
            }
        }
    }

    /** Lets the screen dismiss the inline error banner. */
    fun clearError() {
        _errorMessage.value = null
    }
}
