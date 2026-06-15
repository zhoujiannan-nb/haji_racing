package com.haji.racing.data.remote.api

import com.haji.racing.data.local.db.dao.UserDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val userDao: UserDao,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 不需要Token的接口
        if (request.url.encodedPath.contains("auth/login") ||
            request.url.encodedPath.contains("auth/register")) {
            return chain.proceed(request)
        }

        // 获取当前用户的Token
        val token = runBlocking {
            userDao.getCurrentUser().first()?.token
        }

        // 如果有Token，添加到请求头
        val newRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}
