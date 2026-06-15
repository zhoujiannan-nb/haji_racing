package com.haji.racing.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haji.racing.data.remote.api.HajiApi
import com.haji.racing.data.remote.dto.LoginRequest
import com.haji.racing.data.remote.dto.RegisterRequest
import com.haji.racing.domain.model.User
import com.haji.racing.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: HajiApi,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().first()?.let {
                _isLoggedIn.value = true
            }
        }
    }

    fun login(account: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = api.login(LoginRequest(account, password))
                if (response.code == 0 && response.data != null) {
                    val loginResponse = response.data
                    val user = User(
                        uid = loginResponse.uid,
                        account = account,
                        nickname = loginResponse.nickname,
                        avatarUrl = loginResponse.avatarUrl,
                        token = loginResponse.token,
                        totalDistance = 0.0,
                        totalRecordings = 0,
                        isSynced = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                    userRepository.saveUser(user)
                    _isLoggedIn.value = true
                } else {
                    _errorMessage.value = response.message ?: "登录失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(account: String, password: String, nickname: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = api.register(RegisterRequest(account, password, nickname))
                if (response.code == 0) {
                    // 注册成功，自动登录
                    login(account, password)
                } else {
                    _errorMessage.value = response.message ?: "注册失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
