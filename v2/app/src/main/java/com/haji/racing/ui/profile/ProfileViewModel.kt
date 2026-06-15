package com.haji.racing.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.data.repository.SyncResult
import com.haji.racing.data.repository.SyncService
import com.haji.racing.domain.repository.RecordingRepository
import com.haji.racing.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val recordingRepository: RecordingRepository,
    private val syncService: SyncService,
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                _isLoggedIn.value = user != null
                if (user != null) {
                    _totalDistance.value = recordingRepository.getTotalDistanceForUser(user.uid)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            currentUser.value?.let { userRepository.deleteUser(it.uid) }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                userRepository.updateUser(user.copy(nickname = nickname, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                _lastSyncResult.value = syncService.syncAll()
            } catch (_: Exception) {
                _lastSyncResult.value = SyncResult()
            } finally {
                _isSyncing.value = false
                // 刷新总里程
                currentUser.value?.let { user ->
                    _totalDistance.value = recordingRepository.getTotalDistanceForUser(user.uid)
                }
            }
        }
    }
}
