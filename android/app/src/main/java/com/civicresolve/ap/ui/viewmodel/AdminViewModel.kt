package com.civicresolve.ap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.civicresolve.ap.data.model.AllComplaintsData
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.data.model.Pagination
import com.civicresolve.ap.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminStatsState(
    val data: AllComplaintsData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AdminListState(
    val complaints: List<Complaint> = emptyList(),
    val pagination: Pagination? = null,
    val isLoading: Boolean = false
)

class AdminViewModel(private val repo: ComplaintRepository) : ViewModel() {
    private val _stats = MutableStateFlow(AdminStatsState())
    val stats: StateFlow<AdminStatsState> = _stats

    private val _list = MutableStateFlow(AdminListState())
    val list: StateFlow<AdminListState> = _list

    fun fetchStats() {
        viewModelScope.launch {
            _stats.value = _stats.value.copy(isLoading = true)
            val r = repo.getAdminStats()
            r.onSuccess { res -> _stats.value = AdminStatsState(data = res.stats, isLoading = false) }
                .onFailure { _stats.value = AdminStatsState(isLoading = false, error = it.message) }
        }
    }

    fun fetchComplaints(page: Int = 1, status: String = "all", search: String = "") {
        viewModelScope.launch {
            _list.value = _list.value.copy(isLoading = true)
            val s = if (status == "all") null else status
            val q = if (search.isBlank()) null else search
            val r = repo.getAdminPaginatedComplaints(page, 10, s, q)
            r.onSuccess { res -> _list.value = AdminListState(complaints = res.complaints ?: emptyList(), pagination = res.pagination, isLoading = false) }
                .onFailure { _list.value = AdminListState(isLoading = false) }
        }
    }

    fun fetchAdminChats(page: Int = 1, status: String = "all", onResult: (List<Complaint>, Pagination?) -> Unit) {
        viewModelScope.launch {
            val s = if (status == "all") null else status
            val r = repo.getAdminActiveChats(page, 10, s)
            r.onSuccess { res -> onResult(res.complaints ?: emptyList(), res.pagination) }
                .onFailure { onResult(emptyList(), null) }
        }
    }
}

class AdminViewModelFactory(private val repo: ComplaintRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return AdminViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
