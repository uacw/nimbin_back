package tech.nimbus.models.request

import kotlinx.serialization.Serializable

/**
 * DTO для запроса регистрации пользователя.
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)
