package tech.nimbus.shared.dto.request

import kotlinx.serialization.Serializable
import tech.nimbus.shared.dto.PasteVisibility

/**
 * DTO для запроса создания новой заметки.
 *
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property visibility Тип видимости заметки
 * @property expiresAt Дата и время автоудаления заметки в ISO формате (null = бессрочная)
 * @property language Язык программирования для подсветки синтаксиса
 */
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext"
)

/**
 * DTO для запроса регистрации пользователя.
 *
 * @property username Имя пользователя
 * @property email Email адрес
 * @property password Пароль
 */
@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)

/**
 * DTO для запроса входа в систему.
 *
 * @property email Email адрес для входа
 * @property password Пароль
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

/**
 * DTO для запроса обновления профиля пользователя.
 *
 * @property username Новое имя пользователя (опционально)
 * @property displayName Новое отображаемое имя (опционально)
 */
@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)
