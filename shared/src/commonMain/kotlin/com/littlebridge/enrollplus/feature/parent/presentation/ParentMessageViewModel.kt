package com.littlebridge.enrollplus.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentMessageDto
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentMessageThreadDto
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentRecipientDto
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentSendMessageRequest
import com.littlebridge.enrollplus.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * RA-51: real, network-backed parent messaging. REPLACES the old hardcoded
 * three-fake-threads stub (LAW: MOCKV2 IS DEAD — no fabricated data on a
 * production path). Backs the parent inbox list + an open conversation, and
 * lets the parent reply to a thread the school/teacher started.
 *
 * Three states per LAW are surfaced for both the thread list and the open
 * conversation: loading / error / empty.
 */
data class ParentMessageState(
    // inbox
    val threads: List<ParentMessageThreadDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,

    // open conversation
    val openThreadId: String? = null,
    val openThreadName: String = "",
    val messages: List<ParentMessageDto> = emptyList(),
    val conversationLoading: Boolean = false,
    val conversationError: String? = null,

    // compose (reply)
    val sending: Boolean = false,
    val sendError: String? = null,
    val replyError: String? = null,

    // RA-S07 — compose-NEW (start a conversation with a teacher/admin)
    val composeOpen: Boolean = false,
    val composeLoadingRecipients: Boolean = false,
    val composeRecipients: List<ParentRecipientDto> = emptyList(),
    val composeError: String? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && threads.isEmpty()
    val conversationEmpty: Boolean
        get() = !conversationLoading && conversationError == null && messages.isEmpty()
    val composeEmpty: Boolean
        get() = !composeLoadingRecipients && composeError == null && composeRecipients.isEmpty()
}

class ParentMessageViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }

    private val _state = MutableStateFlow(ParentMessageState())
    val state: StateFlow<ParentMessageState> = _state.asStateFlow()

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
                    // opening clears the unread badge server-side; refresh the list.
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
            it.copy(openThreadId = null, openThreadName = "", messages = emptyList(), conversationError = null, sendError = null, replyError = null)
        }
    }

    /**
     * Reloads the currently-open conversation's messages WITHOUT clearing them
     * first (unlike [openThread]). This prevents UI flicker after a successful
     * reply and during real-time polling.
     */
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
                    // P1-5: Surface polling errors to the UI.
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

    /** Reply to the currently-open thread. */
    fun reply(body: String) {
        val threadId = _state.value.openThreadId ?: return
        if (body.isBlank()) return

        // P1-3: Optimistic send — insert a temp message immediately for instant UI feedback.
        val tempId = "optimistic-${kotlin.random.Random.nextLong()}"
        val optimisticMsg = ParentMessageDto(
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
            val req = ParentSendMessageRequest(
                threadId = threadId,
                body = body.trim(),
                clientMsgId = kotlin.random.Random.nextLong().toString(),
            )
            when (val r = repository.sendMessage(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(sending = false, replyError = null) }
                    // Reload without clearing — no flicker.
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

    /** Clears the inline reply error banner. */
    fun clearReplyError() {
        _state.update { it.copy(replyError = null) }
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

    /**
     * RA-S07 — open the compose-NEW sheet and load recipient candidates (the child's class
     * teacher(s) + the school's admin desk). The parent can now INITIATE contact, not just reply.
     */
    fun openCompose() {
        _state.update {
            it.copy(composeOpen = true, composeLoadingRecipients = true, composeError = null, composeRecipients = emptyList())
        }
        viewModelScope.launch {
            val token = token() ?: run {
                _state.update { it.copy(composeLoadingRecipients = false, composeError = "Not signed in") }
                return@launch
            }
            when (val r = repository.getMessageRecipients(token)) {
                is NetworkResult.Success ->
                    _state.update {
                        it.copy(
                            composeLoadingRecipients = false,
                            composeRecipients = r.data.data?.recipients ?: emptyList(),
                        )
                    }
                is NetworkResult.Error ->
                    _state.update { it.copy(composeLoadingRecipients = false, composeError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(composeLoadingRecipients = false, composeError = "Connection error") }
            }
        }
    }

    /** RA-S07 — dismiss the compose-new sheet. */
    fun closeCompose() {
        _state.update {
            it.copy(composeOpen = false, composeLoadingRecipients = false, composeRecipients = emptyList(), composeError = null)
        }
    }

    /**
     * RA-S07 — start a NEW conversation with [recipientUserId]. On success we close the sheet,
     * refresh the inbox, and open the freshly-created thread so the parent lands inside it.
     */
    fun composeNew(recipientUserId: String, body: String) {
        if (recipientUserId.isBlank()) { _state.update { it.copy(composeError = "Pick a recipient") }; return }
        if (body.isBlank()) { _state.update { it.copy(composeError = "Message cannot be empty") }; return }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, composeError = null) }
            val token = token() ?: run {
                _state.update { it.copy(sending = false, composeError = "Not signed in") }
                return@launch
            }
            val req = ParentSendMessageRequest(
                recipientUserId = recipientUserId,
                body = body.trim(),
                clientMsgId = kotlin.random.Random.nextLong().toString(),
            )
            when (val r = repository.sendMessage(token, req)) {
                is NetworkResult.Success -> {
                    val newThreadId = r.data.data?.threadId
                    val recipientName = _state.value.composeRecipients.firstOrNull { it.id == recipientUserId }?.name ?: ""
                    _state.update {
                        it.copy(
                            sending = false,
                            composeOpen = false,
                            composeRecipients = emptyList(),
                            composeError = null,
                        )
                    }
                    if (newThreadId != null) {
                        // openThread() already calls loadThreads() — no double fetch.
                        openThread(newThreadId, recipientName)
                    } else {
                        // Fallback: refresh threads so the new conversation appears.
                        loadThreads()
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(sending = false, composeError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(sending = false, composeError = "Connection error") }
            }
        }
    }
}
