package com.civicresolve.ap.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.civicresolve.ap.MainActivity
import com.civicresolve.ap.R
import com.civicresolve.ap.data.remote.CookieJarImpl
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * Keeps a dedicated Socket.IO connection for the logged-in user: joins the
 * personal notification room and surfaces `new_notification` /
 * `globalToast` events as system notifications. Without this, in-app
 * notifications (status changes, admin replies, upvotes, points) are
 * silently lost while the user is outside the relevant screen.
 */
class NotificationSocketManager(
    private val context: Context,
    private val cookieJar: CookieJarImpl,
    private val socketUrl: String
) {
    private var socket: Socket? = null

    companion object {
        const val CHANNEL_ID = "civicresolve_notifications"
        const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "CivicResolve updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Complaint status changes, replies and rewards" }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun connect(userId: String) {
        if (socket?.connected() == true) return
        val token = cookieJar.getToken() ?: return
        disconnect()

        val opts = IO.Options().apply { auth = mapOf("token" to token) }
        socket = IO.socket(socketUrl, opts)
        socket?.on(Socket.EVENT_CONNECT) {
            // Server derives the room from the authenticated socket identity
            socket?.emit("join_room", userId)
        }
        socket?.on("new_notification") { args ->
            if (args.isNotEmpty()) {
                try {
                    val json = args[0] as JSONObject
                    show(json.optString("title", "CivicResolve"), json.optString("message"))
                } catch (_: Exception) {}
            }
        }
        socket?.on("globalToast") { args ->
            if (args.isNotEmpty()) {
                try {
                    val json = args[0] as JSONObject
                    show("CivicResolve", json.optString("message"))
                } catch (_: Exception) {}
            }
        }
        socket?.connect()
    }

    fun disconnect() {
        socket?.off(Socket.EVENT_CONNECT)
        socket?.off("new_notification")
        socket?.off("globalToast")
        socket?.disconnect()
        socket = null
    }

    private fun show(title: String, message: String) {
        if (!hasPermission()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "CivicResolve" })
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(message.hashCode(), notification)
        } catch (_: SecurityException) {}
    }
}
