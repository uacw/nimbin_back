package tech.nimbus.database.repositories.interfaces

import tech.nimbus.models.User

/**
 * Интерфейс репозитория для работы с пользователями.
 * Определяет контракт для всех операций с пользователями.
 */
interface IUserRepository {

    /**
     * Создает нового пользователя.
     */
    suspend fun createUser(id: String, username: String, email: String, password: String): Boolean

    /**
     * Получает пользователя по ID.
     */
    suspend fun getUserById(userId: String): User?

    /**
     * Находит пользователя по username.
     */
    suspend fun findByUsername(username: String): User?

    /**
     * Находит пользователя по email.
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Находит пользователя по username (альтернативный метод).
     */
    suspend fun findUserByUsername(username: String): User?

    /**
     * Находит пользователя по email (альтернативный метод).
     */
    suspend fun findUserByEmail(email: String): User?

    /**
     * Проверяет пароль пользователя по ID.
     */
    suspend fun verifyPassword(userId: String, password: String): Boolean

    /**
     * Проверяет пароль по хешу (используется в AuthRoutes).
     */
    suspend fun verifyPasswordByHash(password: String, passwordHash: String): Boolean

    /**
     * Обновляет профиль пользователя.
     */
    suspend fun updateUserProfile(userId: String, username: String?, displayName: String?): User?

    /**
     * Получает количество публичных заметок пользователя.
     */
    suspend fun getUserPublicPastesCount(userId: String): Int

    /**
     * Получает общее количество заметок пользователя.
     */
    suspend fun getUserTotalPastesCount(userId: String): Int
}
