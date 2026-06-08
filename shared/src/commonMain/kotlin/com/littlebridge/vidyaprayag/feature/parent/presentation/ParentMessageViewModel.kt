package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentMessageDto
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentMessageThreadDto
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentSendMessageRequest
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    // compose
    val sending: Boolean = false,
    val sendError: String? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && threads.isEmpty()
    val conversationEmpty: Boolean
        get() = !conversationLoading && conversationError == null && messages.isEmpty()
}

class ParentMessageViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentMessageState())
    val state: StateFlow<ParentMessageState> = _state.asStateFlow()

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
        viewModelScope.launch {
            _state.update {
                it.copy(
                    openThreadId = threadId,
                    openThreadName = fallbackName,
                    messages = emptyList(),
                    conversationLoading = true,
                    conversationError = null,
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
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(conversationLoading = false, conversationError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(conversationLoading = false, conversationError = "Connection error") }
            }
        }
    }

    fun closeThread() {
        _state.update {
            it.copy(openThreadId = null, openThreadName = "", messages = emptyList(), conversationError = null, sendError = null)
        }
    }

    /** Reply to the currently-open thread. */
    fun reply(body: String) {
        val threadId = _state.value.openThreadId ?: return
        if (body.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, sendError = null) }
            val token = token() ?: run {
                _state.update { it.copy(sending = false, sendError = "Not signed in") }
                return@launch
            }
            val req = ParentSendMessageRequest(threadId = threadId, body = body.trim())
            when (val r = repository.sendMessage(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(sending = false) }
                    // re-fetch the conversation so the new message appears with server timestamp.
                    openThread(threadId, _state.value.openThreadName)
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(sending = false, sendError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(sending = false, sendError = "Connection error") }
            }
        }
    }
}
