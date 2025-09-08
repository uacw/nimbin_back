package tech.nimbus.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import tech.nimbus.models.PasteVisibility
import java.time.LocalDateTime

/**
 * Таблица заметок в базе данных.
 *
 * Схема таблицы:
 * - id: VARCHAR(12) PRIMARY KEY
 * - title: VARCHAR(255)
 * - content: TEXT
 * - user_id: VARCHAR(36) NULL
 * - visibility: VARCHAR(20) DEFAULT 'PUBLIC'
 * - created_at: TIMESTAMP DEFAULT NOW()
 * - updated_at: TIMESTAMP DEFAULT NOW()
 * - expires_at: TIMESTAMP NULL
 * - syntax_language: VARCHAR(50) DEFAULT 'plaintext'
 * - view_count: INTEGER DEFAULT 0
 */
object PasteTable : Table("pastes") {
    val id        = varchar("id", 12)
    val title     = varchar("title", 255)
    val content   = text("content")
    val userId    = varchar("user_id", 36).nullable()
    val visibility = enumerationByName<PasteVisibility>("visibility", 20).default(PasteVisibility.PUBLIC)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val expiresAt = datetime("expires_at").nullable()

    /** Язык подсветки синтаксиса */
    val syntaxLanguage  = varchar("syntax_language", 50).default("plaintext")

    val viewCount = integer("view_count").default(0)

    override val primaryKey = PrimaryKey(id)
}