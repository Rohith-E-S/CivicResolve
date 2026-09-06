package com.civicresolve.ap.data.repository

import com.civicresolve.ap.data.model.*
import com.civicresolve.ap.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ComplaintRepository(private val apiService: ApiService) {

    private fun parseError(errorBody: String?): String {
        return try {
            val json = JSONObject(errorBody ?: "")
            json.optString("message", "An error occurred")
        } catch (e: Exception) { "An error occurred" }
    }

    suspend fun createComplaint(
        description: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        city: RequestBody,
        state: RequestBody,
        landmark: RequestBody,
        category: RequestBody?,
        imageUrl: MultipartBody.Part?
    ): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.createComplaint(description, latitude, longitude, city, state, landmark, category, imageUrl)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getComplaint(id: String): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getComplaint(id)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPublicStats(district: String? = null): Result<PublicStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getPublicStats(district)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPublicFeed(district: String? = null, page: Int? = null, limit: Int? = null): Result<ComplaintListResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getPublicFeed(district, page, limit)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rateComplaint(id: String, rating: Int): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.rateComplaint(id, mapOf("rating" to rating))
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMyComplaintStats(): Result<UserComplaintStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getMyComplaintStats()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMyPaginatedComplaints(page: Int? = null, limit: Int? = null, status: String? = null): Result<PaginatedComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getMyPaginatedComplaints(page, limit, status)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMyActiveChats(page: Int? = null, limit: Int? = null, status: String? = null): Result<ActiveChatResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getMyActiveChats(page, limit, status)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAdminStats(): Result<AdminComplaintStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getAdminComplaintStats()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAdminPaginatedComplaints(page: Int? = null, limit: Int? = null, status: String? = null, search: String? = null): Result<PaginatedComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getAdminPaginatedComplaints(page, limit, status, search)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAdminActiveChats(page: Int? = null, limit: Int? = null, status: String? = null): Result<ActiveChatResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getAdminActiveChats(page, limit, status)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getNearbyComplaints(lat: Double, lng: Double, radius: Int = 500): Result<NearbyComplaintsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getNearbyComplaints(lat, lng, radius)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateComplaintStatusWithImage(id: String, status: String?, image: MultipartBody.Part?): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val statusBody = status?.let { it.toRequestBody("text/plain".toMediaType()) }
            val r = apiService.updateComplaintStatusWithImage(id, statusBody, image)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMessages(complaintId: String): Result<MessageListResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getMessages(complaintId)
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Legacy fallbacks
    suspend fun getMyComplaints(): Result<ComplaintListResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getMyComplaints()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getAllComplaints(): Result<AllComplaintsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiService.getAllComplaints()
            if (r.isSuccessful && r.body() != null) Result.success(r.body()!!) else Result.failure(Exception(parseError(r.errorBody()?.string())))
        } catch (e: Exception) { Result.failure(e) }
    }
}
