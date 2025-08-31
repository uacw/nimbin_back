package tech.nimbus.database.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import tech.nimbus.database.tables.UserTable
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.models.User
import tech.nimbus.models.UserPublicInfo
import tech.nimbus.models.PasteVisibility
import at.favre.lib.crypto.bcrypt.BCrypt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Репоз����торий для работы с пользователями.
 */
class UserRepository {

    /**
     * Находит пользователя по username и возвращает пару (userId, passwordHash).
     */
    fun findByUsername(username: String): Pair<String, String>? = transaction {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .singleOrNull()
            ?.let { row ->
                Pair(row[UserTable.id], row[UserTable.passwordHash])
            }
    }

    /**
     * Находит пользователя по email и возвращает пару (userId, passwordHash).
     */
    fun findByEmail(email: String): Pair<String, String>? = transaction {
        UserTable.selectAll()
            .where { UserTable.email eq email }
            .singleOrNull()
            ?.let { row ->
                Pair(row[UserTable.id], row[UserTable.passwordHash])
            }
    }

    /**
     * Создает нового пользователя с указанными параметрами.
     */
    fun createUser(userId: String, username: String, email: String, password: String): Boolean = transaction {
        try {
            val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
            UserTable.insert {
                it[id] = userId
                it[UserTable.username] = username
                it[UserTable.email] = email
                it[UserTable.passwordHash] = passwordHash
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получает пользователя по ID.
     */
    fun getUserById(userId: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.id eq userId }
            .singleOrNull()
            ?.let { User.fromResultRow(it) }
    }

    /**
     * Создает нового пользователя с объектом User.
     */
    fun createUser(user: User): User? = transaction {
        try {
            UserTable.insert {
                it[id] = user.id
                it[username] = user.username
                it[email] = user.email
                it[displayName] = user.displayName
                it[passwordHash] = user.passwordHash
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Получает пользователя по имени ��ользователя.
     */
    fun getUserByUsername(username: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .singleOrNull()
            ?.let { User.fromResultRow(it) }
    }

    /**
     * Получает пользователя по email.
     */
    fun getUserByEmail(email: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.email eq email }
            .singleOrNull()
            ?.let { User.fromResultRow(it) }
    }

    /**
     * Получает публичную информацию о пользоват��ле с количеством публичных заметок.
     */
    fun getUserPublicInfo(userId: String): UserPublicInfo? = transaction {
        val user = getUserById(userId) ?: return@transaction null

        val publicPastesCount = PasteTable.selectAll()
            .where {
                (PasteTable.userId eq userId) and
                (PasteTable.visibility eq PasteVisibility.PUBLIC)
            }
            .count()
            .toInt()

        UserPublicInfo.fromUser(user, publicPastesCount)
    }

    /**
     * Обновляет профиль пользователя.
     */
    fun updateUserProfile(
        userId: String,
        username: String? = null,
        displayName: String? = null
    ): Boolean = transaction {
        try {
            val updateCount = UserTable.update({ UserTable.id eq userId }) {
                username?.let { newUsername -> it[UserTable.username] = newUsername }
                displayName?.let { newDisplayName -> it[UserTable.displayName] = newDisplayName }
            }
            updateCount > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверяет, существует ли пользователь с данным username.
     */
    fun usernameExists(username: String): Boolean = transaction {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .count() > 0
    }

    /**
     * Проверяет, существует ли пользователь с данным email.
     */
    fun emailExists(email: String): Boolean = transaction {
        UserTable.selectAll()
            .where { UserTable.email eq email }
            .count() > 0
    }

    /**
     * Находит пользователя по username и возвращает объект User.
     */
    fun findUserByUsername(username: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .singleOrNull()
            ?.let { User.fromResultRow(it) }
    }

    /**
     * Находит пользователя по email и возвращает объект User.
     */
    fun findUserByEmail(email: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.email eq email }
            .singleOrNull()
            ?.let { User.fromResultRow(it) }
    }

    /**
     * Проверяет пароль пользователя.
     */
    fun verifyPassword(password: String, passwordHash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified
    }
}
