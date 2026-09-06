package com.civicresolve.ap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class ComplaintDetailState(
    val complaint: Complaint? = null,
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ComplaintDetailViewModel(private val repo: ComplaintRepository) : ViewModel() {
    private val _state = MutableStateFlow(ComplaintDetailState())
    val state: StateFlow<ComplaintDetailState> = _state

    fun fetchComplaint(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val r = repo.getComplaint(id)
            r.onSuccess { res ->
                if (res.success && res.complaint != null) {
                    _state.value = _state.value.copy(complaint = res.complaint, isAdmin = res.isAdmin == true, isLoading = false)
                } else _state.value = _state.value.copy(isLoading = false, error = res.message ?: "Failed to load")
            }.onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun rateComplaint(id: String, rating: Int) {
        viewModelScope.launch {
            val r = repo.rateComplaint(id, rating)
            r.onSuccess { res ->
                if (res.success) {
                    _state.value = _state.value.copy(complaint = _state.value.complaint?.copy(rating = rating), successMessage = "Rated $rating/5")
                } else _state.value = _state.value.copy(error = res.message)
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun updateStatusWithImage(id: String, status: String?, file: File?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, error = null, successMessage = null)
            var part: MultipartBody.Part? = null
            if (file != null) {
                val req = file.asRequestBody("image/*".toMediaType())
                part = MultipartBody.Part.createFormData("imageUrl", file.name, req)
            }
            // Backend expects single endpoint for status + image; if only status, we still need part? Send dummy if needed
            // If file is null and status != null, create empty? Instead use status-only if possible via fallback
            val r = if (part != null) repo.updateComplaintStatusWithImage(id, status, part) else {
                // No image but status change: create empty file part handling? We'll send with null part - need overload
                // Workaround: if no image, create status update via _state's current complaint's afterImageUrl check
                repo.updateComplaintStatusWithImage(id, status, null)
            }
            r.onSuccess { res ->
                if (res.success) {
                    _state.value = _state.value.copy(complaint = res.complaint, isUpdating = false, successMessage = "Updated")
                } else _state.value = _state.value.copy(isUpdating = false, error = res.message)
            }.onFailure { _state.value = _state.value.copy(isUpdating = false, error = it.message) }
        }
    }
}

class ComplaintDetailViewModelFactory(private val repo: ComplaintRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComplaintDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ComplaintDetailViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
