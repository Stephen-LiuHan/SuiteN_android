package space.suiten.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import space.suiten.domain.repository.AuthRepository
import space.suiten.domain.repository.SyncRepository
import space.suiten.domain.repository.SyncResult

data class SyncUiState(
    val isLoggedIn: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncAt: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false
)

class SyncViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        observeSyncState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collectLatest { isLoggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncRepository.isSyncing.collectLatest { isSyncing ->
                _uiState.value = _uiState.value.copy(isSyncing = isSyncing)
            }
        }
        viewModelScope.launch {
            syncRepository.lastSyncAt.collectLatest { lastSyncAt ->
                _uiState.value = _uiState.value.copy(lastSyncAt = lastSyncAt)
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.register(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Auto-sync after registration
                    sync()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Registration failed"
                    )
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Auto-sync after login
                    sync()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.logout()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Logout failed"
                    )
                }
            )
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, syncSuccess = false)
            val result = syncRepository.sync()
            handleSyncResult(result)
        }
    }

    fun forceFullSync() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, syncSuccess = false)
            val result = syncRepository.forceFullSync()
            handleSyncResult(result)
        }
    }

    private fun handleSyncResult(result: SyncResult) {
        if (result.success) {
            _uiState.value = _uiState.value.copy(
                syncSuccess = true,
                lastSyncAt = result.syncedAt
            )
            if (result.conflicts.isNotEmpty()) {
                // Handle conflicts if needed (for now, just log)
                _uiState.value = _uiState.value.copy(
                    error = "${result.conflicts.size} conflicts detected"
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                error = result.error ?: "Sync failed"
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.deleteAccount()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Delete account failed"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSyncSuccess() {
        _uiState.value = _uiState.value.copy(syncSuccess = false)
    }
}
