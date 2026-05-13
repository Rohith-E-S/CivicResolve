package com.example.complaintportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.model.*
import com.example.complaintportal.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.example.complaintportal.ui.screens.SortOption

data class ComplaintState(
    val isLoading: Boolean = false,
    val newComplaints: List<Complaint> = emptyList(),
    val inProgressComplaints: List<Complaint> = emptyList(),
    val pendingVerificationComplaints: List<Complaint> = emptyList(),
    val disputedComplaints: List<Complaint> = emptyList(),
    val resolvedComplaints: List<Complaint> = emptyList(),
    val communityComplaints: List<Complaint> = emptyList(),
    val currentComplaint: Complaint? = null,
    val error: String? = null,
    val supportedIds: Set<String> = emptySet(),
    val communityResolvedCount: Int = 1200,
    val statsScope: String = "Global",
    val isCommunityLoading: Boolean = false,
    val aiResult: AiAnalysisResult? = null,
    val pendingComplaintData: PendingComplaintData? = null,
    val nearbyComplaints: List<NearbyComplaint> = emptyList(),
    val sortOption: SortOption = SortOption.DATE_DESC,
    
    val verifyLoading: Boolean = false,
    val disputeLoading: Boolean = false,
    val verifyError: String? = null,
    val disputeError: String? = null
)

data class PendingComplaintData(
    val description: String,
    val lat: String,
    val lng: String,
    val city: String,
    val state: String,
    val landmark: String,
    val imageUri: android.net.Uri
)



class ComplaintViewModel(private val repository: ComplaintRepository) : ViewModel() {

    private val _state = MutableStateFlow(ComplaintState())
    val state: StateFlow<ComplaintState> = _state.asStateFlow()

    fun updateSortOption(option: SortOption) {
        _state.value = _state.value.copy(sortOption = option)
    }

    fun fetchUserComplaints(userId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getMyComplaints()
            result.onSuccess { response ->
                if (response.success) {
                    val complaints = response.complaints ?: emptyList()
                    
                    // Extract supported IDs from the complaints themselves if userId is provided
                    val newSupportedIds = if (userId != null) {
                        complaints.filter { it.supporters?.contains(userId) == true }.map { it.id }.toSet()
                    } else emptySet()
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        supportedIds = _state.value.supportedIds + newSupportedIds,
                        newComplaints = complaints.filter { it.status.lowercase() in listOf("new", "under_review") },
                        inProgressComplaints = complaints.filter { it.status.lowercase() in listOf("in_progress", "re_opened") },
                        pendingVerificationComplaints = complaints.filter { it.status.lowercase() == "pending_verification" },
                        disputedComplaints = complaints.filter { it.status.lowercase() == "disputed" },
                        resolvedComplaints = complaints.filter { it.status.lowercase() in listOf("resolved", "confirmed_resolved") }
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun fetchAdminComplaints(userId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getAllComplaints()
            result.onSuccess { response ->
                if (response.success) {
                    val data = response.complaints

                    // Extract supported IDs
                    val allForSupport = (data?.newComplaint ?: emptyList()) +
                                       (data?.underReviewComplaint ?: emptyList()) +
                                       (data?.inProgressComplaint ?: emptyList()) +
                                       (data?.pendingVerificationComplaint ?: emptyList()) +
                                       (data?.disputedComplaint ?: emptyList()) +
                                       (data?.resolvedComplaint ?: emptyList())
                    val newSupportedIds = if (userId != null) {
                        allForSupport.filter { it.supporters?.contains(userId) == true }.map { it.id }.toSet()
                    } else emptySet()

                    _state.value = _state.value.copy(
                        isLoading = false,
                        supportedIds = _state.value.supportedIds + newSupportedIds,
                        newComplaints = (data?.newComplaint ?: emptyList()) + (data?.underReviewComplaint ?: emptyList()),
                        inProgressComplaints = data?.inProgressComplaint ?: emptyList(),
                        pendingVerificationComplaints = data?.pendingVerificationComplaint ?: emptyList(),
                        disputedComplaints = data?.disputedComplaint ?: emptyList(),
                        resolvedComplaints = data?.resolvedComplaint ?: emptyList()
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
        category: RequestBody?,
        imageUrl: MultipartBody.Part?,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.createComplaint(
                description, latitude, longitude, city, stateBody, landmark, category, imageUrl
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

    fun analyzeImage(imageUrl: MultipartBody.Part) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, aiResult = null)
            val result = repository.analyzeImage(imageUrl)
            result.onSuccess { aiResult ->
                _state.value = _state.value.copy(isLoading = false, aiResult = aiResult)
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun clearAiResult() {
        _state.value = _state.value.copy(aiResult = null)
    }

    fun setPendingComplaintData(data: PendingComplaintData) {
        _state.value = _state.value.copy(pendingComplaintData = data)
    }



    fun fetchComplaint(id: String, userId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getComplaint(id)
            result.onSuccess { response ->
                if (response.success) {
                    val complaint = response.complaint
                    
                    // Update supportedIds if user is a supporter
                    if (complaint != null && userId != null && complaint.supporters?.contains(userId) == true) {
                        _state.value = _state.value.copy(
                            supportedIds = _state.value.supportedIds + complaint.id
                        )
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

    fun fetchCommunityFeed(district: String? = null, userId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCommunityLoading = true)
            val result = repository.getPublicFeed(district)
            result.onSuccess { response ->
                if (response.success) {
                    val complaints = response.complaints ?: emptyList()
                    
                    // Extract supported IDs
                    val newSupportedIds = if (userId != null) {
                        complaints.filter { it.supporters?.contains(userId) == true }.map { it.id }.toSet()
                    } else emptySet()

                    _state.value = _state.value.copy(
                        isCommunityLoading = false,
                        supportedIds = _state.value.supportedIds + newSupportedIds,
                        communityComplaints = complaints
                    )
                } else {
                    _state.value = _state.value.copy(isCommunityLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isCommunityLoading = false, error = it.message)
            }
        }
    }

    fun fetchNearbyComplaints(lat: Double, lng: Double, radius: Int = 500) {
        viewModelScope.launch {
            val result = repository.getNearbyComplaints(lat, lng, radius)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(nearbyComplaints = response.complaints)
                }
            }
        }
    }

    fun verifyComplaint(id: String, lat: Double, lng: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(verifyLoading = true, verifyError = null)
            val result = repository.verifyComplaint(id, lat, lng)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(
                        verifyLoading = false,
                        currentComplaint = response.complaint
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(verifyLoading = false, verifyError = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(verifyLoading = false, verifyError = it.message)
            }
        }
    }

    fun disputeComplaint(
        id: String,
        lat: RequestBody,
        lng: RequestBody,
        description: RequestBody,
        photo: MultipartBody.Part,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(disputeLoading = true, disputeError = null)
            val result = repository.disputeComplaint(id, lat, lng, description, photo)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(
                        disputeLoading = false,
                        currentComplaint = response.complaint
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(disputeLoading = false, disputeError = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(disputeLoading = false, disputeError = it.message)
            }
        }
    }

    fun resolveDispute(id: String, action: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.resolveDispute(id, action)
            result.onSuccess { response ->
                if (response.success) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentComplaint = response.complaint
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message)
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun clearNearbyComplaints() {
        _state.value = _state.value.copy(nearbyComplaints = emptyList())
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
