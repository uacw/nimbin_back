package tech.nimbus.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Таблица избранных заметок пользователя.
 * Составной первичный ключ (user_id, paste_id).
 */
object UserFavoritesTable : Table("user_favorites") {
    val userId  = varchar("user_id", 36)
    val pasteId = varchar("paste_id", 12)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(userId, pasteId)
}

