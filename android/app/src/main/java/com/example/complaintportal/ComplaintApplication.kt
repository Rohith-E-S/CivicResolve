package com.example.complaintportal

import android.app.Application
import com.example.complaintportal.di.AppContainer
import com.example.complaintportal.di.DefaultAppContainer
import com.example.complaintportal.util.NotificationHelper
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ComplaintApplication : Application() {
    lateinit var container: AppContainer
    lateinit var notificationHelper: NotificationHelper

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var globalSocket: Socket? = null
    
    private val knownStatuses = mutableMapOf<String, String>()
    private val joinedRooms = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        notificationHelper = NotificationHelper(this)

        startBackgroundPolling()
    }

    private fun startBackgroundPolling() {
        applicationScope.launch {
            // Initial delay to allow login/app start
            delay(5000)
            while (true) {
                try {
                    val token = container.cookieJar.getToken()
                    if (!token.isNullOrEmpty()) {
                        val authResult = container.authRepository.checkAuth()
                        if (authResult.isSuccess) {
                            val isAdmin = authResult.getOrNull()?.user?.isAdmin == true
                            val complaints = mutableListOf<com.example.complaintportal.data.model.Complaint>()
                            
                            if (isAdmin) {
                                val result = container.complaintRepository.getAllComplaints()
                                if (result.isSuccess) {
                                    val data = result.getOrNull()?.complaints
                                    data?.newComplaint?.let { complaints.addAll(it) }
                                    data?.inProgressComplaint?.let { complaints.addAll(it) }
                                    data?.resolvedComplaint?.let { complaints.addAll(it) }
                                }
                            } else {
                                val result = container.complaintRepository.getMyComplaints()
                                if (result.isSuccess) {
                                    result.getOrNull()?.complaints?.let { complaints.addAll(it) }
                                }
                            }

                            if (globalSocket == null) {
                                val opts = IO.Options().apply { auth = mapOf("token" to token) }
                                globalSocket = IO.socket(container.socketUrl, opts)
                                
                                globalSocket?.on("newMessage") { args ->
                                    if (args.isNotEmpty()) {
                                        try {
                                            val dataString = args[0].toString()
                                            val json = JSONObject(dataString)
                                            val sender = json.getJSONObject("fromUser").getString("fullName")
                                            val msgText = json.getString("message")
                                            val cid = json.getString("complaintId")
                                            notificationHelper.showNotification(
                                                "New Message ($sender)", 
                                                "Case #${cid.takeLast(6).uppercase()}: $msgText"
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                globalSocket?.on("globalToast") { args ->
                                    if (args.isNotEmpty()) {
                                        try {
                                            val json = JSONObject(args[0].toString())
                                            val message = json.getString("message")
                                            // Since we're not in an Activity, we'll show a system notification
                                            notificationHelper.showNotification("CivicResolve Update", message)
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }
                                globalSocket?.connect()
                            }

                            for (c in complaints) {
                                val oldStatus = knownStatuses[c.id]
                                if (oldStatus != null && oldStatus != c.status) {
                                    if (c.status.equals("in progress", true) || c.status.equals("resolved", true)) {
                                        val title = "Complaint Status Updated"
                                        val msg = "Case #${c.id.takeLast(6).uppercase()} is now ${c.status.uppercase()}"
                                        notificationHelper.showNotification(title, msg)
                                    }
                                }
                                knownStatuses[c.id] = c.status
                                
                                if (!joinedRooms.contains(c.id) && !c.status.equals("resolved", true)) {
                                    globalSocket?.emit("joinComplaint", c.id)
                                    joinedRooms.add(c.id)
                                }
                            }
                        }
                    } else {
                        // Logged out
                        knownStatuses.clear()
                        joinedRooms.clear()
                        globalSocket?.disconnect()
                        globalSocket = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10000)
            }
        }
    }
}
