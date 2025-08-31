package tech.nimbus.shared.dto

import kotlinx.serialization.Serializable

/**
 * DTO модель пользователя для multiplatform использования.
 *
 * @property id Уникальный идентификатор пользователя
 * @property username Имя пользователя
 * @property displayName Отображаемое имя пользователя
 * @property email Email адрес пользователя
 * @property createdAt Дата регистрации в формате ISO string
 */
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val email: String,
    val createdAt: String
)
