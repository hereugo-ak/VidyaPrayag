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
package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.Message
import com.littlebridge.enrollplus.feature.admin.domain.model.MessageThread
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.MessagesRepository
import com.littlebridge.enrollplus.feature.admin.domain.repository.TeachersRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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

/**
 * UI state for an opened conversation (the thread detail view).
 *
 * `threadId` non-null means a conversation sheet/screen is open. `isLoading`
 * covers the initial fetch; `isSending` is the in-flight reply. Messages are
 * ordered oldest-first (server orders ASC by created_at).
 */
data class ConversationState(
    val threadId: String? = null,
    val senderName: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

/**
 * RA-S07 — compose-new state. A recipient candidate is any teacher in the admin's school
 * (`GET /api/v1/school/messages` accepts `recipient_user_id`; `TeacherAccountDto.id` IS the
 * recipient's app_users id). `isOpen` drives the compose sheet; `candidates` is the picker list.
 */
data class ComposeState(
    val isOpen: Boolean = false,
    val isLoadingRecipients: Boolean = false,
    val candidates: List<MessageRecipient> = emptyList(),
    val error: String? = null,
)

/** A pickable recipient for compose-new (id = app_users id used as recipient_user_id). */
data class MessageRecipient(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageUrl: String? = null,
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository,
    private val preferenceRepository: PreferenceRepository,
    private val teachersRepository: TeachersRepository,
) : ViewModel() {

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }

    private val _state = MutableStateFlow(MessagesState())
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    private val _compose = MutableStateFlow(ComposeState())
    val compose: StateFlow<ComposeState> = _compose.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _conversation = MutableStateFlow(ConversationState())
    val conversation: StateFlow<ConversationState> = _conversation.asStateFlow()

    private var pollJob: Job? = null

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
        recipientUserId: String? = null,
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

            val request = SendMessageRequest(
                threadId = threadId,
                recipientUserId = recipientUserId,
                body = body,
                clientMsgId = java.util.UUID.randomUUID().toString(),
            )
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

    /**
     * Opens the conversation view for [threadId] and loads its messages.
     * The server clears the unread badge when we fetch, so we also patch the
     * local thread list to drop the badge immediately.
     */
    fun openConversation(threadId: String) {
        stopPolling()
        val thread = _state.value.threads.firstOrNull { it.id == threadId }
        _conversation.value = ConversationState(
            threadId = threadId,
            senderName = thread?.senderName ?: "",
            isLoading = true
        )

        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _conversation.value = _conversation.value.copy(
                    isLoading = false,
                    error = "Not signed in"
                )
                return@launch
            }

            when (val result = messagesRepository.getThreadMessages(token, threadId)) {
                is NetworkResult.Success -> {
                    _conversation.value = _conversation.value.copy(
                        senderName = result.data.senderName,
                        messages = result.data.messages,
                        isLoading = false,
                        error = null
                    )
                    // Opening clears unread on the server; mirror it locally.
                    _state.value = _state.value.copy(
                        threads = _state.value.threads.map {
                            if (it.id == threadId) it.copy(isRead = true, unreadCount = 0) else it
                        }
                    )
                    startPolling()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("MessagesVM", "getThreadMessages failed: ${result.message}")
                    _conversation.value = _conversation.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("MessagesVM", "getThreadMessages connection error")
                    _conversation.value = _conversation.value.copy(
                        isLoading = false,
                        error = "Connection error"
                    )
                }
            }
        }
    }

    /** Reloads the currently open conversation (after sending a reply or via polling). */
    private fun reloadConversation() {
        val threadId = _conversation.value.threadId ?: return
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = messagesRepository.getThreadMessages(token, threadId)) {
                is NetworkResult.Success -> {
                    _conversation.value = _conversation.value.copy(
                        senderName = result.data.senderName,
                        messages = result.data.messages,
                        error = null,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("MessagesVM", "reloadConversation failed: ${result.message}")
                    // P1-5: Surface polling errors to the UI instead of silently swallowing.
                    if (_conversation.value.error == null) {
                        _conversation.value = _conversation.value.copy(error = result.message)
                    }
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("MessagesVM", "reloadConversation connection error")
                    if (_conversation.value.error == null) {
                        _conversation.value = _conversation.value.copy(error = "Connection error")
                    }
                }
            }
        }
    }

    /**
     * Sends a reply within the currently-open conversation, then reloads the
     * conversation and the thread list so previews/timestamps stay accurate.
     */
    fun sendReply(body: String) {
        val threadId = _conversation.value.threadId ?: return
        if (body.isBlank()) {
            _conversation.value = _conversation.value.copy(error = "Message cannot be empty")
            return
        }

        // P1-3: Optimistic send — insert a temp message immediately for instant UI feedback.
        val tempId = "optimistic-${System.currentTimeMillis()}"
        val optimisticMsg = Message(
            id = tempId,
            body = body,
            isMine = true,
            createdAt = "",
            time = "Now",
            status = "SENT",
        )
        _conversation.value = _conversation.value.copy(
            messages = _conversation.value.messages + optimisticMsg,
            isSending = true,
            error = null,
        )

        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _conversation.value = _conversation.value.copy(
                    isSending = false,
                    error = "Not signed in"
                )
                return@launch
            }

            val clientMsgId = java.util.UUID.randomUUID().toString()
            val request = SendMessageRequest(
                threadId = threadId,
                body = body,
                clientMsgId = clientMsgId,
            )
            when (val result = messagesRepository.sendMessage(token, request)) {
                is NetworkResult.Success -> {
                    // Replace optimistic message with real one via reload.
                    _conversation.value = _conversation.value.copy(isSending = false)
                    reloadConversation()
                    refresh()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("MessagesVM", "sendReply failed: ${result.message}")
                    // Remove optimistic message on failure.
                    _conversation.value = _conversation.value.copy(
                        messages = _conversation.value.messages.filterNot { it.id == tempId },
                        isSending = false,
                        error = result.message,
                    )
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("MessagesVM", "sendReply connection error")
                    _conversation.value = _conversation.value.copy(
                        messages = _conversation.value.messages.filterNot { it.id == tempId },
                        isSending = false,
                        error = "Connection error",
                    )
                }
            }
        }
    }

    /** Closes the conversation view and resets its state. */
    fun closeConversation() {
        stopPolling()
        _conversation.value = ConversationState()
    }

    /**
     * RA-S07 — open the compose-new sheet and load recipient candidates.
     * Uses the dedicated /recipients endpoint which returns both staff
     * (teachers + admins) and parents (whose children are enrolled).
     */
    fun openCompose() {
        _compose.value = ComposeState(isOpen = true, isLoadingRecipients = true)
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _compose.value = _compose.value.copy(isLoadingRecipients = false, error = "Not signed in")
                return@launch
            }
            when (val result = messagesRepository.getRecipients(token)) {
                is NetworkResult.Success -> {
                    val candidates = result.data.map {
                        MessageRecipient(
                            id = it.id,
                            name = it.name,
                            subtitle = it.subtitle,
                            imageUrl = it.imageUrl,
                        )
                    }
                    _compose.value = _compose.value.copy(isLoadingRecipients = false, candidates = candidates, error = null)
                }
                is NetworkResult.Error -> _compose.value = _compose.value.copy(isLoadingRecipients = false, error = result.message)
                is NetworkResult.ConnectionError -> _compose.value = _compose.value.copy(isLoadingRecipients = false, error = "Connection error")
            }
        }
    }

    /** RA-S07 — dismiss the compose sheet. */
    fun closeCompose() {
        _compose.value = ComposeState()
    }

    /**
     * RA-S07 — start a NEW 1:1 conversation with [recipientUserId]. On success the compose sheet
     * closes, threads refresh, and we open the freshly-created thread so the admin lands in it.
     */
    fun composeNew(recipientUserId: String, body: String) {
        if (recipientUserId.isBlank()) { _compose.value = _compose.value.copy(error = "Pick a recipient"); return }
        if (body.isBlank()) { _compose.value = _compose.value.copy(error = "Message cannot be empty"); return }
        sendMessage(body = body, threadId = null, recipientUserId = recipientUserId) {
            closeCompose()
        }
    }

    /** Dismisses an inline error inside the conversation view. */
    fun clearConversationError() {
        _conversation.value = _conversation.value.copy(error = null)
    }

    /** Lets the screen dismiss the inline error banner. */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Starts periodic polling for new messages in the open conversation.
     * Called when a conversation is opened; stopped when it's closed.
     */
    private fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_conversation.value.threadId != null && !_conversation.value.isSending) {
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
