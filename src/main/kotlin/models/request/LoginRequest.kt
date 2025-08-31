package tech.nimbus.models.request

import kotlinx.serialization.Serializable

/**
 * DTO для запроса логина пользователя.
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)
