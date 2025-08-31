package tech.nimbus.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Модель пользователя системы.
 */
@Serializable
data class User(
    val id: String,                    // UUID пользователя (неизменяемый)
    val username: String,              // Имя пользователя (можно изменить)
    val email: String,                 // Email (уникальный)
    val displayName: String? = null,   // Отображаемое имя (можно изменить)
    val passwordHash: String,          // Хэш пароля
    val createdAt: String             // Дата создания аккаунта
) {
    companion object {
        fun fromResultRow(row: org.jetbrains.exposed.sql.ResultRow): User {
            return User(
                id = row[tech.nimbus.database.tables.UserTable.id],
                username = row[tech.nimbus.database.tables.UserTable.username],
                email = row[tech.nimbus.database.tables.UserTable.email],
                displayName = row[tech.nimbus.database.tables.UserTable.displayName],
                passwordHash = row[tech.nimbus.database.tables.UserTable.passwordHash],
                createdAt = row[tech.nimbus.database.tables.UserTable.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }
}

/**
 * DTO для публичной информации о пользователе (без приватных данных).
 */
@Serializable
data class UserPublicInfo(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val createdAt: String,
    val publicPastesCount: Int = 0
) {
    companion object {
        fun fromUser(user: User, publicPastesCount: Int = 0): UserPublicInfo {
            return UserPublicInfo(
                id = user.id,
                username = user.username,
                displayName = user.displayName,
                createdAt = user.createdAt,
                publicPastesCount = publicPastesCount
            )
        }
    }
}
