package tech.nimbus.models.request

import kotlinx.serialization.Serializable
import tech.nimbus.models.PasteVisibility

/**
 * DTO модель для запроса создания новой заметки (локальный вариант).
 * Предпочтительно используйте shared CreatePasteRequestDto в маршрутах.
 *
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property visibility Тип видимости заметки (PUBLIC по умолчанию)
 * @property expiresAt Дата и время автоудаления в ISO (null = бессрочная)
 * @property syntaxLanguage Язык подсветки синтаксиса
 */
@Serializable
data class CreatePasteRequest(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext"
)
