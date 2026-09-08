package com.hangingspider.game.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.hangingspider.game.R
import com.hangingspider.game.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        _state.value = AuthState(uid = repo.currentUser?.uid)
    }

    fun signInWithGoogle(context: Context) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val option = GetSignInWithGoogleOption
                    .Builder(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val response = CredentialManager.create(context).getCredential(context, request)
                val cred = response.credential
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val user = repo.signInWithGoogleIdToken(idToken)
                _state.value = AuthState(uid = user.uid)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message ?: "Sign-in failed")
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthState()
    }

    fun deleteAccount(onDone: (com.hangingspider.game.data.repo.UserRepository.DeleteResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteAccount()
            if (result is com.hangingspider.game.data.repo.UserRepository.DeleteResult.Ok) {
                _state.value = AuthState()
            }
            onDone(result)
        }
    }
}

data class AuthState(
    val uid: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)
