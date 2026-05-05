package com.example.complaintportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.model.*
import com.example.complaintportal.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {

    private val _analyticsData = MutableStateFlow<AnalyticsDataDto?>(null)
    val analyticsData: StateFlow<AnalyticsDataDto?> = _analyticsData.asStateFlow()

    private val _adminAnalyticsData = MutableStateFlow<AdminAnalyticsDataDto?>(null)
    val adminAnalyticsData: StateFlow<AdminAnalyticsDataDto?> = _adminAnalyticsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchAnalytics(period: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getUserAnalytics(period)
                if (response.isSuccessful && response.body()?.success == true) {
                    _analyticsData.value = response.body()?.data
                } else {
                    _error.value = response.message() ?: "Failed to fetch analytics"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAdminAnalytics(period: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getAdminAnalytics(period)
                if (response.isSuccessful && response.body()?.success == true) {
                    _adminAnalyticsData.value = response.body()?.data
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { 
                        // Try to parse JSON error message if possible
                        try {
                            val json = org.json.JSONObject(it)
                            json.optString("message", "")
                        } catch (e: Exception) { null }
                    } ?: response.message()
                    
                    _error.value = if (errorMsg.isNullOrBlank()) "Error ${response.code()}: Access Denied or Server Error" else errorMsg
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error - check connection"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AnalyticsViewModelFactory(private val repository: AnalyticsRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
