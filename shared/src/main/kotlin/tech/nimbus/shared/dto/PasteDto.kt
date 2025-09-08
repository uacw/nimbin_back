package tech.nimbus.shared.dto

import kotlinx.serialization.Serializable

/**
 * DTO модель текстовой заметки для multiplatform использования.
 *
 * Эта модель может использоваться как в backend (Ktor), так и в Android/iOS приложениях.
 * Использует kotlinx.datetime для кроссплатформенной работы с датами.
 *
 * @property id Уникальный 12-символьный идентификатор заметки
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property userId ID владельца заметки (null для анонимных заметок)
 * @property authorUsername Имя пользователя автора (null для анонимных)
 * @property authorDisplayName Отображаемое имя автора (null для анонимных)
 * @property visibility Тип видимости заметки
 * @property createdAt Дата и время создания в формате ISO string
 * @property updatedAt Дата и время последнего обновления в формате ISO string
 * @property expiresAt Дата и время автоудаления (null = не удалять)
 * @property language Язык программирования для подсветки синтаксиса
 * @property viewCount Счетчик просмотров заметки
 * @property etag Этикетка версии заметки (для оптимистичной блокировки)
 */
@Serializable
data class PasteDto(
    val id: String,
    val title: String,
    val content: String,
    val userId: String? = null,
    val authorUsername: String? = null,
    val authorDisplayName: String? = null,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val createdAt: String,
    val updatedAt: String,
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext",
    val viewCount: Int = 0,
    val etag: String? = null
)
