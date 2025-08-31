package tech.nimbus.shared.dto

import kotlinx.serialization.Serializable

/**
 * DTO для ответа аутентификации.
 *
 * @property token JWT токен для авторизации
 * @property user Информация о пользователе
 */
@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

/**
 * DTO для ответа удаления заметки.
 *
 * @property message Сообщение об успешном удалении
 */
@Serializable
data class DeleteResponseDto(
    val message: String
)

/**
 * DTO для ошибок API.
 *
 * @property error Сообщение об ошибке
 * @property code Код ошибки (опционально)
 */
@Serializable
data class ApiErrorDto(
    val error: String,
    val code: String? = null
)

/**
 * DTO для профиля пользователя с дополнительной статистикой.
 *
 * @property user Основная информация о пользователе
 * @property publicPastesCount Количество публичных заметок пользователя
 * @property totalPastesCount Общее количество заметок (только для владельца профиля)
 */
@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int? = null  // null для чужих профилей
)
