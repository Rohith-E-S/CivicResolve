package com.example.complaintportal.data.model

import androidx.compose.ui.graphics.Color


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
    val supporters: List<String>? = emptyList(),
    val createdAt: String?,
    val updatedAt: String?,
    @Json(name = "timestamps") val timestamps: ComplaintTimestamps? = null
)

@JsonClass(generateAdapter = true)
data class ComplaintTimestamps(
    @Json(name = "reported")    val reported:    String? = null,
    @Json(name = "underReview") val underReview: String? = null,
    @Json(name = "inProgress")  val inProgress:  String? = null,
    @Json(name = "resolved")    val resolved:    String? = null,
)

@JsonClass(generateAdapter = true)
data class NearbyComplaint(
    @Json(name = "_id")            val id:              String,
    @Json(name = "category")       val category:        String,
    @Json(name = "description")    val description:     String,
    @Json(name = "status")         val status:          String,
    @Json(name = "distanceMeters") val distanceMeters:  Int,
    @Json(name = "createdAt")      val createdAt:       String,
)

@JsonClass(generateAdapter = true)
data class NearbyComplaintsResponse(
    @Json(name = "success")    val success:    Boolean,
    @Json(name = "count")      val count:      Int,
    @Json(name = "complaints") val complaints: List<NearbyComplaint>,
)

data class AiAnalysisResult(
    val category: String,
    val confidence: Float,
    val description: String,
    val severity: String,
    val emoji: String
)

data class IssueCategory(
    val id: String,
    val label: String,
    val emoji: String,
    val color: Color
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
    val underReviewComplaint: List<Complaint>?,
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
    val totalActive: Int,
    val scope: String? = null
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

@JsonClass(generateAdapter = true)
data class UpdateStatusRequest(
    val status: String
)
