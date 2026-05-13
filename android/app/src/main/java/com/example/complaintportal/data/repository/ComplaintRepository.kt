package com.example.complaintportal.data.repository

import com.example.complaintportal.data.model.*
import com.example.complaintportal.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject

class ComplaintRepository(private val apiService: ApiService) {

    private fun parseError(errorBody: String?): String {
        return try {
            val json = JSONObject(errorBody ?: "")
            json.optString("message", "An error occurred")
        } catch (e: Exception) {
            "An error occurred"
        }
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
            val response = apiService.createComplaint(
                description, latitude, longitude, city, state, landmark, category, imageUrl
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeImage(imageUrl: MultipartBody.Part): Result<AiAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.analyzeImage(imageUrl)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getMyComplaints(): Result<ComplaintListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyComplaints()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllComplaints(): Result<AllComplaintsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllComplaints()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatus(id: String, status: String): Result<SingleComplaintResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateComplaintStatus(id, UpdateStatusRequest(status))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadAfterImage(id: String, imageUrl: okhttp3.MultipartBody.Part): Result<SingleComplaintResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.uploadAfterImage(id, imageUrl)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun rateComplaint(id: String, rating: Int): Result<BaseResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.rateComplaint(id, mapOf("rating" to rating))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun supportComplaint(id: String): Result<BaseResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.supportComplaint(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getComplaint(id: String): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getComplaint(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicStats(district: String? = null): Result<PublicStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPublicStats(district)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicFeed(district: String? = null): Result<ComplaintListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPublicFeed(district)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyComplaints(lat: Double, lng: Double, radius: Int = 500): Result<NearbyComplaintsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNearbyComplaints(lat, lng, radius)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyComplaint(id: String, lat: Double, lng: Double): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyComplaint(id, mapOf("latitude" to lat, "longitude" to lng))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disputeComplaint(
        id: String,
        lat: RequestBody,
        lng: RequestBody,
        description: RequestBody,
        photo: MultipartBody.Part
    ): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.disputeComplaint(id, lat, lng, description, photo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveDispute(id: String, action: String): Result<SingleComplaintResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resolveDispute(id, mapOf("action" to action))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
