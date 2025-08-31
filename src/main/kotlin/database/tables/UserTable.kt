package tech.nimbus.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Таблица пользователей в базе данных.
 *
 * Определяет схему таблицы "users" с использованием Exposed ORM.
 * Содержит информацию о зарегистрированных пользователях системы
 * с поддержкой уникальности имен пользователей и email адресов.
 *
 * Схема таблицы:
 * - id: VARCHAR(36) PRIMARY KEY - UUID пользователя
 * - username: VARCHAR(50) UNIQUE - имя пользователя
 * - email: VARCHAR(255) UNIQUE - email адрес
 * - password_hash: VARCHAR(255) - bcrypt хеш пароля
 * - created_at: TIMESTAMP DEFAULT NOW() - дата регистрации
 */
object UserTable : Table("users") {
    /** Уникальный UUID идентификатор пользователя (первичный ключ) */
    val id = varchar("id", 36)

    /** Имя пользователя (уникальное, до 50 символов) */
    val username = varchar("username", 50).uniqueIndex()

    /** Email адрес пользователя (уникальный, до 255 символов) */
    val email = varchar("email", 255).uniqueIndex()

    /** Отображаемое имя пользователя (до 100 символов, может быть пустым) */
    val displayName = varchar("display_name", 100).nullable()

    /** bcrypt хеш пароля пользователя */
    val passwordHash = varchar("password_hash", 255)

    /** Дата и время регистрации пользователя */
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
