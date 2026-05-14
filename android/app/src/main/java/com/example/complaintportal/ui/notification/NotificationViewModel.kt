package com.example.complaintportal.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.notification.NotificationApiService
import com.example.complaintportal.data.notification.NotificationItem
import io.socket.client.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount:   Int     = 0,
    val isLoading:     Boolean = false,
    val error:         String? = null,
)

class NotificationViewModel(
    private val api:    NotificationApiService,
    private val socket: Socket,
    private val userId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        setupSocketListeners()
        fetchNotifications()
        startPollingFallback()
    }

    private fun setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT) {
            Log.d("NotificationVM", "Socket connected, joining room: $userId")
            joinSocketRoom()
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("NotificationVM", "Socket connection error: ${args.getOrNull(0)}")
        }
        
        if (socket.connected()) {
            joinSocketRoom()
        }

        listenForRealTimeNotifications()
    }

    private fun joinSocketRoom() {
        Log.d("NotificationVM", "Joining notification room: $userId")
        socket.emit("join_room", userId)
    }

    private fun listenForRealTimeNotifications() {
        socket.on("new_notification") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val item = NotificationItem(
                id          = data.optString("_id"),
                userId      = userId,
                type        = data.optString("type"),
                title       = data.optString("title"),
                message     = data.optString("message"),
                complaintId = data.optString("complaintId").takeIf { it.isNotBlank() },
                isRead      = data.optBoolean("isRead", false),
                createdAt   = data.optString("createdAt"),
            )
            _uiState.update { current ->
                current.copy(
                    notifications = listOf(item) + current.notifications,
                    unreadCount   = current.unreadCount + 1,
                )
            }
        }
    }

    // ── REST calls ────────────────────────────────────────────────────────────

    fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getNotifications(userId)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            notifications = body.notifications,
                            unreadCount   = body.unreadCount,
                            isLoading     = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load notifications") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                api.markAllRead(userId)
                _uiState.update { current ->
                    current.copy(
                        unreadCount   = 0,
                        notifications = current.notifications.map { it.copy(isRead = true) }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun markOneRead(notificationId: String) {
        viewModelScope.launch {
            try {
                api.markOneRead(notificationId)
                _uiState.update { current ->
                    val updated = current.notifications.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                    current.copy(
                        notifications = updated,
                        unreadCount   = maxOf(0, current.unreadCount - 1),
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                api.clearAll(userId)
                _uiState.update { it.copy(notifications = emptyList(), unreadCount = 0) }
            } catch (_: Exception) {}
        }
    }

    fun clearOne(notificationId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    val updated = state.notifications.filter { it.id != notificationId }
                    state.copy(
                        notifications = updated,
                        unreadCount = updated.count { !it.isRead }
                    )
                }
                // Assuming there's a backend endpoint to delete, if not this just handles UI locally
                runCatching { api.deleteNotification(notificationId) }
            } catch (_: Exception) {}
        }
    }

    // ── Polling fallback every 30 s ───────────────────────────────────────────

    private fun startPollingFallback() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                try {
                    val response = api.getUnreadCount(userId)
                    if (response.isSuccessful) {
                        val count = response.body()?.unreadCount ?: return@launch
                        if (count != _uiState.value.unreadCount) fetchNotifications()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket.off("new_notification")
        socket.off(Socket.EVENT_CONNECT)
        socket.off(Socket.EVENT_CONNECT_ERROR)
        socket.disconnect()
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

class NotificationViewModelFactory(
    private val api:    NotificationApiService,
    private val socket: Socket,
    private val userId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationViewModel(api, socket, userId) as T
}
