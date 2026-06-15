package com.haji.racing.data.repository

import com.haji.racing.data.local.db.dao.UserDao
import com.haji.racing.data.local.db.entity.UserEntity
import com.haji.racing.domain.model.User
import com.haji.racing.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> =
        userDao.getCurrentUser().map { it?.toDomain() }

    override suspend fun getUserByUid(uid: String): User? =
        userDao.getUserByUid(uid)?.toDomain()

    override suspend fun saveUser(user: User) =
        userDao.insertUser(user.toEntity())

    override suspend fun updateUser(user: User) =
        userDao.updateUser(user.toEntity())

    override suspend fun deleteUser(uid: String) =
        userDao.deleteUser(uid)

    private fun UserEntity.toDomain() = User(
        uid = uid, account = account, nickname = nickname, avatarUrl = avatarUrl,
        token = token, totalDistance = totalDistance, totalRecordings = totalRecordings,
        isSynced = isSynced, createdAt = createdAt, updatedAt = updatedAt,
    )

    private fun User.toEntity() = UserEntity(
        uid = uid, account = account, nickname = nickname, avatarUrl = avatarUrl,
        token = token, totalDistance = totalDistance, totalRecordings = totalRecordings,
        isSynced = isSynced, createdAt = createdAt, updatedAt = updatedAt,
    )
}
