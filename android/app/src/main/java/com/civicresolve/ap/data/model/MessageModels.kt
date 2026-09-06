package com.civicresolve.ap.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "_id") val id: String?,
    val complaintId: String?,
    val fromUser: User?,
    val toUser: User?,
    val message: String?,
    val hasSeen: Boolean? = false,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class MessageListResponse(
    val success: Boolean,
    val message: String?,
    val messages: List<Message>?
)
