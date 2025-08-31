package tech.nimbus.models.request

import kotlinx.serialization.Serializable

/**
 * DTO для обновления профиля пользователя.
 */
@Serializable
data class UpdateProfileRequest(
    val username: String? = null,      // Новое имя пользователя
    val displayName: String? = null    // Новое отображаемое имя
)
