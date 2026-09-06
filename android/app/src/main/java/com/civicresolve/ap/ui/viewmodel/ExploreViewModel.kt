package com.civicresolve.ap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.data.model.Pagination
import com.civicresolve.ap.data.model.PublicStats
import com.civicresolve.ap.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExploreState(
    val complaints: List<Complaint> = emptyList(),
    val stats: PublicStats? = null,
    val isLoading: Boolean = false,
    val pagination: Pagination? = null,
    val error: String? = null
)

class ExploreViewModel(private val repo: ComplaintRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state

    fun fetchFeed(district: String?, page: Int = 1, limit: Int = 20) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val d = if (district.isNullOrBlank() || district == "all") "all" else district
            val r = repo.getPublicFeed(d, page, limit)
            r.onSuccess { res ->
                if (res.success) {
                    _state.value = _state.value.copy(complaints = res.complaints ?: emptyList(), isLoading = false)
                    // Pagination not returned by ComplaintListResponse for feed; keep existing
                } else _state.value = _state.value.copy(isLoading = false, error = res.message)
            }.onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun fetchStats(district: String?) {
        viewModelScope.launch {
            val d = if (district.isNullOrBlank() || district == "all") "all" else district
            val r = repo.getPublicStats(d)
            r.onSuccess { res -> _state.value = _state.value.copy(stats = res.stats) }
        }
    }

    // For Explore pagination via feed's hidden pagination; we reuse same call with page param
    fun fetchFeedPaginated(district: String?, page: Int, limit: Int = 20) = fetchFeed(district, page, limit)
}

class ExploreViewModelFactory(private val repo: ComplaintRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ExploreViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
