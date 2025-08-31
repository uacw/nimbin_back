package tech.nimbus.models

import kotlinx.serialization.Serializable

/**
 * Информация об авторе заметки для отображения в клиенте.
 */
@Serializable
data class AuthorInfo(
    val id: String,
    val username: String,
    val displayName: String? = null
)

/**
 * Расширенный DTO для заметки с информацией об авторе.
 *
 * Используется для сериализации заметок в JSON ответах API.
 * Содержит все необходимые поля для отображения заметки в клиентском приложении
 * с поддержкой трех типов видимости.
 *
 * @property id Уникальный идентификатор заметки без трудноразличимых символов
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property visibility Тип видимости заметки (PUBLIC, UNLISTED, PRIVATE)
 * @property createdAt Дата создания в ISO формате
 * @property expiresAt Дата истечения срока действия заметки в ISO формате (null, если заметка бессрочная)
 * @property language Язык для подсветки синтаксиса
 * @property viewCount Количество просмотров
 * @property author Информация об авторе заметки (null для анонимных заметок)
 */
@Serializable
data class PasteResponse(
    val id: String,
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val createdAt: String,
    val expiresAt: String? = null,
    val language: String = "text",
    val viewCount: Int = 0,
    val author: AuthorInfo? = null  // null для анонимных заметок
) {
    companion object {
        /**
         * Создает PasteResponse из Paste без информации об авторе (для анонимных заметок).
         */
        fun fromPaste(paste: Paste): PasteResponse {
            return PasteResponse(
                id = paste.id,
                title = paste.title,
                content = paste.content,
                visibility = paste.visibility,
                createdAt = paste.createdAt,
                expiresAt = paste.expiresAt,
                language = paste.language,
                viewCount = paste.viewCount,
                author = null
            )
        }

        /**
         * Создает PasteResponse из Paste с информацией об авторе.
         */
        fun fromPasteWithAuthor(paste: Paste, author: User): PasteResponse {
            return PasteResponse(
                id = paste.id,
                title = paste.title,
                content = paste.content,
                visibility = paste.visibility,
                createdAt = paste.createdAt,
                expiresAt = paste.expiresAt,
                language = paste.language,
                viewCount = paste.viewCount,
                author = AuthorInfo(
                    id = author.id,
                    username = author.username,
                    displayName = author.displayName
                )
            )
        }
    }
}
