package com.example.complaintportal.data.remote

import com.example.complaintportal.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ---- Auth APIs ----
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<BaseResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<BaseResponse>

    @POST("auth/create-account")
    suspend fun createAccount(@Body request: CreateAccountRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/sendPasswordResetOtp")
    suspend fun sendPasswordResetOtp(@Body request: SendPasswordResetOtpRequest): Response<BaseResponse>

    @POST("auth/verifyPasswordResetOtp")
    suspend fun verifyPasswordResetOtp(@Body request: VerifyPasswordResetOtpRequest): Response<VerifyPasswordResetOtpResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<BaseResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<BaseResponse>

    @POST("auth/update-home-district")
    suspend fun updateHomeDistrict(@Body request: Map<String, String>): Response<AuthResponse>

    @GET("auth/check-auth")
    suspend fun checkAuth(): Response<AuthResponse>

    @Multipart
    @POST("auth/update")
    suspend fun updateProfile(
        @Part("fullName") fullName: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part profilePic: MultipartBody.Part?
    ): Response<AuthResponse>

    // ---- Complaint APIs ----
    @Multipart
    @POST("complaint/create-complaint")
    suspend fun createComplaint(
        @Part("description") description: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("city") city: RequestBody,
        @Part("state") state: RequestBody,
        @Part("landmark") landmark: RequestBody,
        @Part("category") category: RequestBody?,
        @Part imageUrl: MultipartBody.Part?
    ): Response<SingleComplaintResponse>

    @Multipart
    @POST("complaint/analyze-image")
    suspend fun analyzeImage(
        @Part imageUrl: MultipartBody.Part
    ): Response<AiAnalysisResult>


    @GET("complaint/get-complaints")
    suspend fun getMyComplaints(): Response<ComplaintListResponse>

    @GET("complaint/get-all-complaints")
    suspend fun getAllComplaints(): Response<AllComplaintsResponse>

    @POST("complaint/update-complaint-status/{id}")
    suspend fun updateComplaintStatus(
        @Path("id") id: String,
        @Body request: UpdateStatusRequest
    ): Response<SingleComplaintResponse>

    @Multipart
    @POST("complaint/upload-after-image/{id}")
    suspend fun uploadAfterImage(
        @Path("id") id: String,
        @Part imageUrl: MultipartBody.Part
    ): Response<SingleComplaintResponse>
    
    @Multipart
    @POST("complaint/update-complaint-status-upload-image/{id}")
    suspend fun updateComplaintStatusWithImage(
        @Path("id") id: String,
        @Part("status") status: RequestBody?,
        @Part imageUrl: MultipartBody.Part
    ): Response<SingleComplaintResponse>

    @GET("complaint/get-complaint-data/{id}")
    suspend fun getComplaint(@Path("id") id: String): Response<SingleComplaintResponse>

    @GET("complaint/feed")
    suspend fun getPublicFeed(@Query("district") district: String? = null): Response<ComplaintListResponse>

    @GET("complaint/public-stats")
    suspend fun getPublicStats(@Query("district") district: String? = null): Response<PublicStatsResponse>

    @POST("complaint/rate/{id}")
    suspend fun rateComplaint(
        @Path("id") id: String,
        @Body request: Map<String, Int>
    ): Response<BaseResponse>

    @POST("complaint/support/{id}")
    suspend fun supportComplaint(@Path("id") id: String): Response<BaseResponse>

    @GET("complaint/admin/stats")
    suspend fun getAdminComplaintStats(): Response<AdminComplaintStatsResponse>

    @GET("complaint/admin/active-chats")
    suspend fun getAdminActiveChats(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null
    ): Response<ActiveChatResponse>

    @GET("complaint/admin/list")
    suspend fun getAdminPaginatedComplaints(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<PaginatedComplaintResponse>

    @GET("complaint/my-stats")
    suspend fun getMyComplaintStats(): Response<UserComplaintStatsResponse>

    @GET("complaint/my-active-chats")
    suspend fun getMyActiveChats(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null
    ): Response<ActiveChatResponse>

    @GET("complaint/my-list")
    suspend fun getMyPaginatedComplaints(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null
    ): Response<PaginatedComplaintResponse>

    @GET("complaint/nearby")
    suspend fun getNearbyComplaints(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 500
    ): Response<NearbyComplaintsResponse>

    // ---- Message APIs ----
    @GET("messages/{complaintId}")
    suspend fun getMessages(@Path("complaintId") complaintId: String): Response<MessageListResponse>

    // ---- Analytics APIs ----
    @GET("complaint/analytics/user")
    suspend fun getUserAnalytics(@Query("period") period: String?): Response<AnalyticsResponse>

    @GET("complaint/analytics/admin")
    suspend fun getAdminAnalytics(@Query("period") period: String?): Response<AdminAnalyticsResponse>
}
