package com.example.presentmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.db.DeveloperIdea
import com.example.presentmate.db.DeveloperIdeaDao
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeveloperIdeasSort {
    NEWEST,
    PRIORITY
}

data class DeveloperIdeasUiState(
    val ideas: List<DeveloperIdea> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val sortBy: DeveloperIdeasSort = DeveloperIdeasSort.NEWEST,
    val totalCount: Int = 0,
    val todoCount: Int = 0,
    val doneCount: Int = 0,
    val bugCount: Int = 0
)

@HiltViewModel
class DeveloperIdeasViewModel @Inject constructor(
    private val developerIdeaDao: DeveloperIdeaDao
) : ViewModel() {

    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedStatus = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow(DeveloperIdeasSort.NEWEST)

    private val _rawIdeas = developerIdeaDao.getAllIdeas(uid)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val uiState: StateFlow<DeveloperIdeasUiState> = combine(
        _rawIdeas,
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _sortBy
    ) { ideas, search, category, status, sort ->
        val filtered = ideas.filter { idea ->
            val matchesSearch = idea.title.contains(search, ignoreCase = true) ||
                    idea.description.contains(search, ignoreCase = true)
            val matchesCategory = category == null || idea.category == category
            val matchesStatus = status == null || idea.status == status

            matchesSearch && matchesCategory && matchesStatus
        }.let { list ->
            when (sort) {
                DeveloperIdeasSort.NEWEST -> list.sortedByDescending { it.createdAt }
                DeveloperIdeasSort.PRIORITY -> list.sortedBy { idea ->
                    when (idea.priority) {
                        "High" -> 0
                        "Medium" -> 1
                        "Low" -> 2
                        else -> 3
                    }
                }
            }
        }

        DeveloperIdeasUiState(
            ideas = filtered,
            searchQuery = search,
            selectedCategory = category,
            selectedStatus = status,
            sortBy = sort,
            totalCount = ideas.size,
            todoCount = ideas.count { it.status == "Todo" },
            doneCount = ideas.count { it.status == "Done" },
            bugCount = ideas.count { it.category == "Bug" && it.status == "Todo" }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeveloperIdeasUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
    }

    fun setSortBy(sort: DeveloperIdeasSort) {
        _sortBy.value = sort
    }

    fun addIdea(title: String, description: String, category: String, priority: String) {
        viewModelScope.launch {
            val newIdea = DeveloperIdea(
                userId = uid,
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority,
                status = "Todo"
            )
            developerIdeaDao.insertIdea(newIdea)
        }
    }

    fun toggleIdeaStatus(idea: DeveloperIdea) {
        viewModelScope.launch {
            val newStatus = if (idea.status == "Todo") "Done" else "Todo"
            developerIdeaDao.updateIdea(idea.copy(status = newStatus))
        }
    }

    fun deleteIdea(idea: DeveloperIdea) {
        viewModelScope.launch {
            developerIdeaDao.deleteIdea(idea)
        }
    }
}
