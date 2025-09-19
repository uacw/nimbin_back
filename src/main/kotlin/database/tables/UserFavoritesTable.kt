package tech.nimbus.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Таблица избранных заметок пользователя/гостя.
 * Теперь без составного PK на (user_id, paste_id) — уникальность обеспечивается индексами в SQL-миграции:
 *  - UNIQUE (user_id, paste_id) WHERE user_id IS NOT NULL
 *  - UNIQUE (guest_id, paste_id) WHERE guest_id IS NOT NULL
 */
object UserFavoritesTable : Table("user_favorites") {
    // Для пользователей — может быть NULL в гостевом режиме
    val userId  = varchar("user_id", 36).nullable()
    val pasteId = varchar("paste_id", 12)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    // Столбец для гостевого избранного
    val guestId = varchar("guest_id", 36).nullable()

    // Первичный ключ не задаём здесь — он управляется миграцией БД (может быть surrogate PK или его отсутствие)
}
