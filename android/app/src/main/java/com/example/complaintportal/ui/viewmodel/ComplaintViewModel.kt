package com.example.complaintportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.model.Complaint
import com.example.complaintportal.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class ComplaintState(
    val isLoading: Boolean = false,
    val newComplaints: List<Complaint> = emptyList(),
    val inProgressComplaints: List<Complaint> = emptyList(),
    val resolvedComplaints: List<Complaint> = emptyList(),
    val communityComplaints: List<Complaint> = emptyList(),
    val currentComplaint: Complaint? = null,
    val error: String? = null,
    val supportedIds: Set<String> = emptySet(),
    val communityResolvedCount: Int = 1200,
    val statsScope: String = "Global",
    val isCommunityLoading: Boolean = false
)

class ComplaintViewModel(private val repository: ComplaintRepository) : ViewModel() {

    private val _state = MutableStateFlow(ComplaintState())
    val state: StateFlow<ComplaintState> = _state.asStateFlow()

    fun fetchUserComplaints() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getMyComplaints()
            result.onSuccess { response ->
                if (response.success) {
                    val complaints = response.complaints ?: emptyList()
                    val supportedIds = _state.value.supportedIds
                    
                    val processedComplaints = complaints.map { 
                        if (it.id in supportedIds && (it.supportCount ?: 0) == 0) it.copy(supportCount = 1) else it
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        newComplaints = processedComplaints.filter { it.status.lowercase() == "new" },
                        inProgressComplaints = processedComplaints.filter { it.status.lowercase() == "in progress" },
                        resolvedComplaints = processedComplaints.filter { it.status.lowercase() == "resolved" }
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun fetchAdminComplaints() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getAllComplaints()
            result.onSuccess { response ->
                if (response.success) {
                    val data = response.complaints
                    val supportedIds = _state.value.supportedIds
                    
                    val process = { list: List<Complaint>? ->
                        list?.map { 
                            if (it.id in supportedIds && (it.supportCount ?: 0) == 0) it.copy(supportCount = 1) else it
                        } ?: emptyList()
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        newComplaints = process(data?.newComplaint),
                        inProgressComplaints = process(data?.inProgressComplaint),
                        resolvedComplaints = process(data?.resolvedComplaint)
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun createComplaint(
        description: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        city: RequestBody,
        stateBody: RequestBody,
        landmark: RequestBody,
        imageUrl: MultipartBody.Part?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.createComplaint(
                description, latitude, longitude, city, stateBody, landmark, imageUrl
            )
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun fetchComplaint(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getComplaint(id)
            result.onSuccess { response ->
                if (response.success) {
                    var complaint = response.complaint
                    if (complaint != null && complaint.id in _state.value.supportedIds) {
                        if ((complaint.supportCount ?: 0) == 0) {
                            complaint = complaint.copy(supportCount = 1)
                        }
                    }
                    _state.value = _state.value.copy(isLoading = false, currentComplaint = complaint)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateComplaintStatus(id: String, status: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.updateComplaintStatus(id, status)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(isLoading = false, currentComplaint = response.complaint)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun uploadAfterImage(id: String, imageUrl: okhttp3.MultipartBody.Part, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.uploadAfterImage(id, imageUrl)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(isLoading = false, currentComplaint = response.complaint)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun rateComplaint(id: String, rating: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.rateComplaint(id, rating)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun supportComplaint(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentState = _state.value
            val isCurrentlySupported = id in currentState.supportedIds
            
            val delta = if (isCurrentlySupported) -1 else 1
            val updatedSupportedIds = if (isCurrentlySupported) {
                currentState.supportedIds - id
            } else {
                currentState.supportedIds + id
            }
            
            val updateList = { list: List<Complaint> ->
                list.map { 
                    if (it.id == id) {
                        val currentCount = it.supportCount ?: 0
                        it.copy(supportCount = maxOf(0, currentCount + delta))
                    } else it 
                }
            }
            
            _state.value = currentState.copy(
                supportedIds = updatedSupportedIds,
                newComplaints = updateList(currentState.newComplaints),
                inProgressComplaints = updateList(currentState.inProgressComplaints),
                resolvedComplaints = updateList(currentState.resolvedComplaints),
                communityComplaints = updateList(currentState.communityComplaints),
                currentComplaint = if (currentState.currentComplaint?.id == id) {
                    val currentCount = currentState.currentComplaint.supportCount ?: 0
                    currentState.currentComplaint.copy(supportCount = maxOf(0, currentCount + delta))
                } else currentState.currentComplaint
            )
            
            val result = repository.supportComplaint(id)
            result.onSuccess { response ->
                if (response.success) {
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    fun fetchPublicStats(district: String? = null) {
        viewModelScope.launch {
            val result = repository.getPublicStats(district)
            result.onSuccess { response ->
                if (response.success && response.stats != null) {
                    _state.value = _state.value.copy(
                        communityResolvedCount = response.stats.totalResolved,
                        statsScope = response.stats.scope ?: (if (district == null) "Nationwide" else district)
                    )
                }
            }
        }
    }

    fun fetchCommunityFeed(district: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCommunityLoading = true)
            val result = repository.getPublicFeed(district)
            result.onSuccess { response ->
                if (response.success) {
                    val complaints = response.complaints ?: emptyList()
                    val supportedIds = _state.value.supportedIds
                    
                    val processed = complaints.map { 
                        if (it.id in supportedIds && (it.supportCount ?: 0) == 0) it.copy(supportCount = 1) else it
                    }

                    _state.value = _state.value.copy(
                        isCommunityLoading = false,
                        communityComplaints = processed
                    )
                } else {
                    _state.value = _state.value.copy(isCommunityLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isCommunityLoading = false, error = it.message)
            }
        }
    }
}

class ComplaintViewModelFactory(private val repository: ComplaintRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComplaintViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComplaintViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
