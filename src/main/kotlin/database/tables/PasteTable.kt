package tech.nimbus.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import tech.nimbus.models.PasteVisibility
import java.time.LocalDateTime

/**
 * Таблица заметок в базе данных.
 *
 * Определяет схему таблицы "pastes" с использованием Exposed ORM.
 * Содержит текстовые заметки пользователей с поддержкой анонимных публикаций,
 * трех типов видимости (PUBLIC, UNLISTED, PRIVATE), автоматического удаления
 * и счетчика просмотров.
 *
 * Схема таблицы:
 * - id: VARCHAR(12) PRIMARY KEY - уникальный ID заметки без трудноразличимых символов
 * - title: VARCHAR(255) - заголовок заметки
 * - content: TEXT - содержимое заметки
 * - user_id: VARCHAR(36) NULL - ID владельца (NULL для анонимных)
 * - visibility: VARCHAR(20) DEFAULT 'PUBLIC' - тип видимости (PUBLIC/UNLISTED/PRIVATE)
 * - created_at: TIMESTAMP DEFAULT NOW() - дата создания
 * - expires_at: TIMESTAMP NULL - дата автоудаления
 * - language: VARCHAR(50) DEFAULT 'text' - язык подсветки синтаксиса
 * - view_count: INTEGER DEFAULT 0 - счетчик просмотров
 */
object PasteTable : Table("pastes") {
    /** Уникальный 12-символьный идентификатор заметки (первичный ключ) */
    val id        = varchar("id", 12)

    /** Заголовок заметки (до 255 символов) */
    val title     = varchar("title", 255)

    /** Текстовое содержимое заметки (неограниченная длина) */
    val content   = text("content")

    /** ID владельца заметки (NULL для анонимных заметок) */
    val userId    = varchar("user_id", 36).nullable()

    /** Тип видимости заметки (PUBLIC, UNLISTED, PRIVATE) */
    val visibility = enumerationByName<PasteVisibility>("visibility", 20).default(PasteVisibility.PUBLIC)

    /** Дата и время создания заметки */
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    /** Дата и время автоматического удаления (NULL = бессрочная) */
    val expiresAt = datetime("expires_at").nullable()

    /** Язык программирования для подсветки синтаксиса */
    val language  = varchar("language", 50).default("text")

    /** Счетчик просмотров заметки */
    val viewCount = integer("view_count").default(0)

    override val primaryKey = PrimaryKey(id)
}