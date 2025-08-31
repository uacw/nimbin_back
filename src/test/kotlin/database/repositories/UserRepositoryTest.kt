package tech.nimbus.database.repositories

import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.User
import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlinx.coroutines.test.runTest

/**
 * Unit тесты для репозитория пользователей.
 * Использует H2 in-memory базу данных для изоляции тестов.
 */
class UserRepositoryTest {

    private lateinit var userRepository: UserRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        // Создаем in-memory H2 базу данных для тестов
        database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        userRepository = UserRepository()

        // Создаем схему таблиц
        transaction(database) {
            SchemaUtils.create(UserTable)
        }
    }

    @AfterEach
    fun cleanup() {
        // Очищаем таблицы после каждого теста
        transaction(database) {
            SchemaUtils.drop(UserTable)
        }
    }

    @Test
    fun `createUser должен создавать пользователя с захешированным паролем`() = runTest {
        // Given
        val userId = "test-user-123"
        val username = "testuser"
        val email = "test@example.com"
        val password = "password123"

        // When
        val success = userRepository.createUser(userId, username, email, password)

        // Then
        org.junit.jupiter.api.Assertions.assertTrue(success, "Создание пользователя должно быть успешным")

        // Проверяем, что пользователь действительно создан
        val foundUser = userRepository.getUserById(userId)
        assertNotNull(foundUser, "Пользователь должен быть найден")
        foundUser!!.let { user ->
            assertEquals(username, user.username)
            assertEquals(email, user.email)
        }
    }

    @Test
    fun `findByUsername должен находить пользователя по имени`() = runTest {
        // Given
        val userId = "test-user-456"
        val username = "testuser2"
        val email = "test2@example.com"
        val password = "password456"

        userRepository.createUser(userId, username, email, password)

        // When
        val result = userRepository.findByUsername(username)

        // Then
        assertNotNull(result, "Пользователь должен быть найден")
        assertEquals(userId, result.first, "ID пользователя должен совпадать")

        // Проверяем, что пароль захеширован правильно
        val passwordHash = result.second
        assertTrue(
            BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified,
            "Хеш пароля должен быть валидным"
        )
    }

    @Test
    fun `findByUsername должен возвращать null для несуществующего пользователя`() = runTest {
        // When
        val result = userRepository.findByUsername("nonexistent")

        // Then
        assertNull(result, "Несуществующий пользователь должен возвращать null")
    }

    @Test
    fun `findByEmail должен находить пользователя по email`() = runTest {
        // Given
        val userId = "test-user-789"
        val username = "testuser3"
        val email = "test3@example.com"
        val password = "password789"

        userRepository.createUser(userId, username, email, password)

        // When
        val result = userRepository.findByEmail(email)

        // Then
        assertNotNull(result, "Пользователь должен быть найден")
        assertEquals(userId, result.first, "ID пользователя должен совпадать")

        // Проверяем хеш пароля
        val passwordHash = result.second
        assertTrue(
            BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified,
            "Хеш пароля должен быть валидным"
        )
    }

    @Test
    fun `findByEmail должен возвращать null для несуществующего email`() = runTest {
        // When
        val result = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertNull(result, "Несуществующий email должен возвращать null")
    }

    @Test
    fun `getUserById должен возвращать полную информацию о пользователе`() = runTest {
        // Given
        val userId = "test-user-full"
        val username = "fulluser"
        val email = "full@example.com"
        val password = "fullpassword"

        userRepository.createUser(userId, username, email, password)

        // When
        val user = userRepository.getUserById(userId)

        // Then
        assertNotNull(user, "Пользователь должен быть найден")
        assertEquals(userId, user.id)
        assertEquals(username, user.username)
        assertEquals(email, user.email)
        assertNotNull(user.createdAt, "Дата создания должна быть установлена")
    }

    @Test
    fun `getUserById должен возвращать null для несуществующего ID`() = runTest {
        // When
        val user = userRepository.getUserById("nonexistent-id")

        // Then
        assertNull(user, "Несуществующий ID должен возвращать null")
    }

    @Test
    fun `создание пользователей с одинаковым username должно вызывать исключение`() = runTest {
        // Given
        val username = "duplicateuser"
        val success1 = userRepository.createUser("user1", username, "email1@example.com", "password1")
        assertTrue(success1, "Первый пользователь должен создаться успешно")

        // When
        val success2 = userRepository.createUser("user2", username, "email2@example.com", "password2")

        // Then
        assertFalse(success2, "Создание пользователя с дублирующим username должно завершиться неудачей")
    }

    @Test
    fun `создание пользователей с одинаковым email должно вызывать исключение`() = runTest {
        // Given
        val email = "duplicate@example.com"
        val success1 = userRepository.createUser("user1", "username1", email, "password1")
        assertTrue(success1, "Первый пользователь должен создаться успешно")

        // When
        val success2 = userRepository.createUser("user2", "username2", email, "password2")

        // Then
        assertFalse(success2, "Создание пользователя с дублирующим email должно завершиться неудачей")
    }

    @Test
    fun `пароли должны хешироваться с использованием bcrypt`() = runTest {
        // Given
        val password = "testpassword123"
        val userId = "bcrypt-test-user"

        userRepository.createUser(userId, "bcryptuser", "bcrypt@example.com", password)

        // When
        val result = userRepository.findByEmail("bcrypt@example.com")

        // Then
        assertNotNull(result)
        val passwordHash = result.second

        // Проверяем формат bcrypt хеша
        assertTrue(passwordHash.startsWith("$2"), "Хеш должен начинаться с $2 (bcrypt)")
        assertTrue(passwordHash.length >= 60, "Bcrypt хеш должен быть не менее 60 символов")

        // Проверяем, что верификация работает
        assertTrue(
            BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified,
            "Пароль должен проходить верификацию"
        )

        // Проверяем, что неверный пароль не проходит
        assertFalse(
            BCrypt.verifyer().verify("wrongpassword".toCharArray(), passwordHash).verified,
            "Неверный пароль не должен проходить верификацию"
        )
    }

    @Test
    fun `должен обрабатывать специальные символы в данных пользователя`() = runTest {
        // Given
        val userId = "unicode-user"
        val username = "пользователь测试"
        val email = "тест@пример.рф"
        val password = "пароль123!@#"

        // When
        userRepository.createUser(userId, username, email, password)

        // Then
        val user = userRepository.getUserById(userId)
        assertNotNull(user)
        assertEquals(username, user.username)
        assertEquals(email, user.email)

        // Проверяем поиск по unicode данным
        val foundByUsername = userRepository.findByUsername(username)
        assertNotNull(foundByUsername)
        assertEquals(userId, foundByUsername.first)

        val foundByEmail = userRepository.findByEmail(email)
        assertNotNull(foundByEmail)
        assertEquals(userId, foundByEmail.first)
    }
}
