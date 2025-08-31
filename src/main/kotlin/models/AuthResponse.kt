package tech.nimbus.models

import kotlinx.serialization.Serializable

/**
 * Ответ на успешную аутентификацию (регистрация или логин).
 */
@Serializable
data class AuthResponse(
    val user: User,
    val token: String
)
