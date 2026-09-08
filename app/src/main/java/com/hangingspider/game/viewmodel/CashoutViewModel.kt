package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangingspider.game.data.model.CashoutRequest
import com.hangingspider.game.data.repo.CashoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CashoutViewModel(
    private val uid: String,
    private val repo: CashoutRepository = CashoutRepository()
) : ViewModel() {

    private val _requests = MutableStateFlow<List<CashoutRequest>>(emptyList())
    val requests: StateFlow<List<CashoutRequest>> = _requests

    private val _event = MutableStateFlow<CashoutEvent?>(null)
    val event: StateFlow<CashoutEvent?> = _event

    init {
        viewModelScope.launch {
            repo.observeUserRequests(uid).collectLatest { _requests.value = it }
        }
    }

    fun submit(rewardId: String, email: String) {
        viewModelScope.launch {
            _event.value = when (val r = repo.createRequest(uid, rewardId, email)) {
                is CashoutRepository.Result.Ok -> CashoutEvent.Submitted(r.requestId)
                is CashoutRepository.Result.NotEnoughCoins -> CashoutEvent.InsufficientCoins(r.have, r.need)
                is CashoutRepository.Result.Failed -> CashoutEvent.Error(r.message)
            }
        }
    }

    fun consumeEvent() { _event.value = null }
}

sealed interface CashoutEvent {
    data class Submitted(val id: String) : CashoutEvent
    data class InsufficientCoins(val have: Long, val need: Long) : CashoutEvent
    data class Error(val message: String) : CashoutEvent
}
