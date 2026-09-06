package com.civicresolve.ap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.data.model.Pagination
import com.civicresolve.ap.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardStats(
    val total: Int = 0,
    val newComplaint: Int = 0,
    val inProgressComplaint: Int = 0,
    val resolvedComplaint: Int = 0
)

data class DashboardState(
    val stats: DashboardStats = DashboardStats(),
    val statsLoading: Boolean = false,
    val complaints: List<Complaint> = emptyList(),
    val complaintsLoading: Boolean = false,
    val pagination: Pagination? = null,
    val error: String? = null
)

class DashboardViewModel(private val repo: ComplaintRepository) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun fetchStats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(statsLoading = true)
            val r = repo.getMyComplaintStats()
            r.onSuccess { res ->
                res.stats?.let { s ->
                    _state.value = _state.value.copy(stats = DashboardStats(s.total, s.newComplaint, s.inProgressComplaint, s.resolvedComplaint), statsLoading = false)
                } ?: run { _state.value = _state.value.copy(statsLoading = false) }
            }.onFailure { _state.value = _state.value.copy(statsLoading = false, error = it.message) }
        }
    }

    fun fetchMyComplaints(page: Int = 1, limit: Int = 10, status: String = "all") {
        viewModelScope.launch {
            _state.value = _state.value.copy(complaintsLoading = true)
            val statusParam = if (status == "all") null else status
            val r = repo.getMyPaginatedComplaints(page, limit, statusParam)
            r.onSuccess { res ->
                if (res.success) {
                    _state.value = _state.value.copy(complaints = res.complaints ?: emptyList(), pagination = res.pagination, complaintsLoading = false)
                } else _state.value = _state.value.copy(complaintsLoading = false, error = res.message)
            }.onFailure { _state.value = _state.value.copy(complaintsLoading = false, error = it.message) }
        }
    }

    fun fetchMyChats(page: Int = 1, limit: Int = 10, status: String = "all", onResult: (List<Complaint>, Pagination?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val statusParam = if (status == "all") null else status
            val r = repo.getMyActiveChats(page, limit, statusParam)
            r.onSuccess { res -> onResult(res.complaints ?: emptyList(), res.pagination) }
            r.onFailure { onResult(emptyList(), null) }
        }
    }
}

class DashboardViewModelFactory(private val repo: ComplaintRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return DashboardViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
