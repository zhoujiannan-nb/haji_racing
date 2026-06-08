package com.haji.racing.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.data.remote.api.HajiApi
import com.haji.racing.domain.model.User
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
    private val api: HajiApi,
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

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

    fun login() {
        viewModelScope.launch {
            val response = api.login(mapOf("username" to "test", "password" to "test"))
            response.data?.let { dto ->
                val user = User(
                    uid = dto.uid, nickname = dto.nickname, avatarUrl = dto.avatarUrl,
                    totalDistance = dto.totalDistance, totalRecordings = dto.totalRecordings,
                    isSynced = true, createdAt = dto.createdAt, updatedAt = dto.updatedAt,
                )
                userRepository.saveUser(user)
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
}
