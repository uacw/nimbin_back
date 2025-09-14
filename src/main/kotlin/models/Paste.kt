package tech.nimbus.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Основная модель текстовой заметки (paste).
 *
 * Представляет заметку в системе с поддержкой как анонимных, так и авторизованных
 * пользователей. Заметки могут быть публичными, приватными или скрытыми (unlisted),
 * с возможностью автоматического удаления по истечении времени.
 *
 * @property id Уникальный 12-символьный идентификатор заметки без трудноразличимых символов
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property userId ID владельца заметки (null для анонимных заметок)
 * @property visibility Тип видимости заметки (PUBLIC, UNLISTED, PRIVATE)
 * @property createdAt Дата и время создания в формате ISO string
 * @property updatedAt Дата и время последнего обновления в формате ISO string
 * @property expiresAt Дата и время автоудаления (null = не удалять)
 * @property language Язык программирования для подсветки синтаксиса
 * @property viewCount Счетчик просмотров заметки
 */
@Serializable
data class Paste(
    val id: String,
    val title: String,
    val content: String,
    val userId: String?,               // null — анонимная заметка
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val createdAt: String,             // ISO-строка
    val updatedAt: String,             // ISO-строка
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext",
    val viewCount: Int = 0,
    // Новый владелец для гостя (вместо userId)
    val guestId: String? = null
)
