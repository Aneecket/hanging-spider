package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangingspider.game.data.model.LeaderboardEntry
import com.hangingspider.game.data.repo.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val repo: LeaderboardRepository = LeaderboardRepository()
) : ViewModel() {

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries

    init {
        viewModelScope.launch {
            repo.observeTop(20).collectLatest { _entries.value = it }
        }
    }
}
