package com.civicresolve.ap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.civicresolve.ap.data.model.Message
import com.civicresolve.ap.data.repository.ComplaintRepository
import com.civicresolve.ap.di.AppContainer
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val repo: ComplaintRepository, private val appContainer: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state
    private var socket: Socket? = null
    private var currentComplaintId: String? = null
    private var currentUserId: String? = null

    fun loadMessages(complaintId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val r = repo.getMessages(complaintId)
            r.onSuccess { res -> _state.value = _state.value.copy(messages = res.messages ?: emptyList(), isLoading = false) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun connect(complaintId: String, currentUserId: String) {
        this.currentComplaintId = complaintId
        this.currentUserId = currentUserId
        val token = appContainer.cookieJar.getToken() ?: return
        val opts = IO.Options().apply { auth = mapOf("token" to token) }
        socket = IO.socket(appContainer.socketUrl, opts)
        socket?.on(Socket.EVENT_CONNECT) {
            socket?.emit("joinComplaint", complaintId)
            socket?.emit("markSeen", JSONObject().apply { put("complaintId", complaintId) })
        }
        socket?.on("newMessage") { args ->
            if (args.isNotEmpty()) {
                try {
                    val json = JSONObject(args[0].toString())
                    val msgId = json.optString("_id")
                    val msgText = json.optString("message")
                    val fromUserJson = json.optJSONObject("fromUser")
                    val toUserJson = json.optJSONObject("toUser")
                    val createdAt = json.optString("createdAt")
                    val hasSeen = json.optBoolean("hasSeen", false)
                    val fromUser = fromUserJson?.let {
                        com.civicresolve.ap.data.model.User(
                            id = it.optString("_id"),
                            email = it.optString("email"),
                            fullName = it.optString("fullName"),
                            profilePic = it.optString("profilePic"),
                            address = null,
                            homeDistrict = null,
                            isAdmin = it.optBoolean("isAdmin", false)
                        )
                    }
                    val toUser = toUserJson?.let {
                        com.civicresolve.ap.data.model.User(
                            id = it.optString("_id"),
                            email = null,
                            fullName = it.optString("fullName"),
                            profilePic = null,
                            address = null,
                            homeDistrict = null,
                            isAdmin = it.optBoolean("isAdmin", false)
                        )
                    }
                    val msg = Message(id = msgId, complaintId = complaintId, fromUser = fromUser, toUser = toUser, message = msgText, hasSeen = hasSeen, createdAt = createdAt, updatedAt = createdAt)
                    _state.value = _state.value.copy(messages = _state.value.messages + msg)
                    socket?.emit("markSeen", JSONObject().apply { put("complaintId", complaintId) })
                } catch (_: Exception) {}
            }
        }
        socket?.on("messagesSeen") { args ->
            if (args.isNotEmpty()) {
                try {
                    val json = JSONObject(args[0].toString())
                    val seenBy = json.optString("seenBy")
                    _state.value = _state.value.copy(messages = _state.value.messages.map { m ->
                        if (m.fromUser?.id == currentUserId && m.toUser?.id == seenBy) m.copy(hasSeen = true) else m
                    })
                } catch (_: Exception) {}
            }
        }
        socket?.connect()
    }

    fun sendMessage(complaintId: String, message: String) {
        // Recipient is resolved server-side (owner for admins, a district
        // admin for citizens) — never send identity data from the client
        socket?.emit("sendMessage", JSONObject().apply {
            put("complaintId", complaintId)
            put("message", message)
        })
    }

    fun disconnect() {
        socket?.off("newMessage")
        socket?.off("messagesSeen")
        socket?.disconnect()
        socket = null
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}

class ChatViewModelFactory(private val repo: ComplaintRepository, private val appContainer: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ChatViewModel(repo, appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
