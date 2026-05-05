package com.example.complaintportal.data.repository

import com.example.complaintportal.data.model.*
import com.example.complaintportal.data.remote.ApiService
import retrofit2.Response

class AnalyticsRepository(private val apiService: ApiService) {
    suspend fun getUserAnalytics(period: String? = null): Response<AnalyticsResponse> {
        return apiService.getUserAnalytics(period)
    }

    suspend fun getAdminAnalytics(period: String? = null): Response<AdminAnalyticsResponse> {
        return apiService.getAdminAnalytics(period)
    }
}
