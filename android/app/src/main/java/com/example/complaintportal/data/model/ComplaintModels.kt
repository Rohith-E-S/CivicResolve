package com.example.complaintportal.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Complaint(
    @Json(name = "_id") val id: String,
    val user: User?, // Populated user object or can be omitted if not populated
    val description: String,
    val latitude: String,
    val longitude: String,
    val city: String,
    val state: String,
    val landmark: String,
    val beforeImageUrl: String?,
    val afterImageUrl: String?,
    val category: String,
    val status: String,
    val rating: Int,
    val supportCount: Int? = 0,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ComplaintListResponse(
    val success: Boolean,
    val message: String?,
    val complaints: List<Complaint>?
)

@JsonClass(generateAdapter = true)
data class AllComplaintsData(
    val newComplaint: List<Complaint>?,
    val inProgressComplaint: List<Complaint>?,
    val resolvedComplaint: List<Complaint>?
)

@JsonClass(generateAdapter = true)
data class AllComplaintsResponse(
    val success: Boolean,
    val message: String?,
    val complaints: AllComplaintsData?
)

@JsonClass(generateAdapter = true)
data class Pagination(
    val total: Int,
    val totalPages: Int,
    val currentPage: Int,
    val limit: Int
)

@JsonClass(generateAdapter = true)
data class PaginatedComplaintResponse(
    val success: Boolean,
    val message: String?,
    val complaints: List<Complaint>?,
    val pagination: Pagination?
)

@JsonClass(generateAdapter = true)
data class UserComplaintStats(
    val total: Int,
    val newComplaint: Int,
    val inProgressComplaint: Int,
    val resolvedComplaint: Int
)

@JsonClass(generateAdapter = true)
data class UserComplaintStatsResponse(
    val success: Boolean,
    val message: String?,
    val stats: UserComplaintStats?
)

@JsonClass(generateAdapter = true)
data class AdminComplaintStatsResponse(
    val success: Boolean,
    val message: String?,
    val stats: AllComplaintsData?
)

@JsonClass(generateAdapter = true)
data class PublicStats(
    val totalResolved: Int,
    val totalActive: Int
)

@JsonClass(generateAdapter = true)
data class PublicStatsResponse(
    val success: Boolean,
    val message: String?,
    val stats: PublicStats?
)

@JsonClass(generateAdapter = true)
data class SingleComplaintResponse(
    val success: Boolean,
    val message: String?,
    val complaint: Complaint?,
    val isAdmin: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ActiveChatResponse(
    val success: Boolean,
    val message: String?,
    val complaints: List<Complaint>?,
    val pagination: Pagination?
)
