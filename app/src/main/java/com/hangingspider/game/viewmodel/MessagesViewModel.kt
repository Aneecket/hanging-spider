package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangingspider.game.data.model.AdminMessage
import com.hangingspider.game.data.repo.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val uid: String,
    private val repo: MessageRepository = MessageRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AdminMessage>>(emptyList())
    val messages: StateFlow<List<AdminMessage>> = _messages

    init {
        viewModelScope.launch {
            repo.observeInbox(uid).collectLatest { _messages.value = it }
        }
    }
}
