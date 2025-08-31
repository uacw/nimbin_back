package tech.nimbus.models.request

import kotlinx.serialization.Serializable
import tech.nimbus.models.PasteVisibility

/**
 * DTO модель для запроса создания новой заметки.
 *
 * Используется для десериализации JSON тела запроса при создании заметки.
 * Поддерживает как анонимное, так и авторизованное создание заметок с тремя
 * уровнями видимости: PUBLIC, UNLISTED, PRIVATE.
 *
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property visibility Тип видимости заметки (PUBLIC по умолчанию)
 * @property expiresAt Дата и время автоудаления заметки в ISO формате (null = бессрочная)
 * @property language Язык программирования для подсветки синтаксиса
 */
@Serializable
data class CreatePasteRequest(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,
    val language: String = "text"
)
