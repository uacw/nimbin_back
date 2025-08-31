package tech.nimbus.database.repositories

import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime

/**
 * Unit тесты для репозитория заметок.
 * Использует H2 in-memory базу данных для изоляции тестов.
 */
class PasteRepositoryTest {

    private lateinit var pasteRepository: PasteRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        // Создаем in-memory H2 базу данных для тестов
        database = Database.connect(
            url = "jdbc:h2:mem:paste_test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        pasteRepository = PasteRepository()

        // Создаем схему таблиц
        transaction(database) {
            SchemaUtils.create(UserTable, PasteTable)
        }
    }

    @AfterEach
    fun cleanup() {
        // Очищаем таблицы после каждого теста
        transaction(database) {
            SchemaUtils.drop(PasteTable, UserTable)
        }
    }

    private fun createTestPaste(
        id: String = "test123",
        title: String = "Test Paste",
        content: String = "Test content",
        userId: String? = "user123",
        visibility: PasteVisibility = PasteVisibility.PUBLIC,
        language: String = "text"
    ): Paste {
        return Paste(
            id = id,
            title = title,
            content = content,
            userId = userId,
            visibility = visibility,
            createdAt = LocalDateTime.now().toString(),
            expiresAt = null,
            language = language,
            viewCount = 0
        )
    }

    @Test
    fun `createPaste должен создавать заметку с правильными данными`() = runTest {
        // Given
        val paste = createTestPaste()

        // When
        val created = pasteRepository.createPaste(paste)

        // Then
        assertNotNull(created, "Созданная заметка не должна быть null")
        assertEquals(paste.id, created.id)
        assertEquals(paste.title, created.title)
        assertEquals(paste.content, created.content)
        assertEquals(paste.userId, created.userId)
        assertEquals(paste.visibility, created.visibility)
        assertEquals(paste.language, created.language)
        assertEquals(0, created.viewCount) // viewCount должен быть сброшен в 0
    }

    @Test
    fun `createPaste должен создавать анонимную заметку`() = runTest {
        // Given
        val paste = createTestPaste(userId = null)

        // When
        val created = pasteRepository.createPaste(paste)

        // Then
        assertNotNull(created)
        assertNull(created.userId, "Анонимная заметка должна иметь userId = null")
    }

    @Test
    fun `getPasteById должен находить заметку по ID`() = runTest {
        // Given
        val paste = createTestPaste()
        pasteRepository.createPaste(paste)

        // When
        val found = pasteRepository.getPasteById(paste.id)

        // Then
        assertNotNull(found, "Заметка должна быть найдена")
        assertEquals(paste.id, found.id)
        assertEquals(paste.title, found.title)
        assertEquals(paste.content, found.content)
    }

    @Test
    fun `getPasteById должен возвращать null для несуществующего ID`() = runTest {
        // When
        val found = pasteRepository.getPasteById("nonexistent")

        // Then
        assertNull(found, "Несуществующая заметка должна возвращать null")
    }

    @Test
    fun `incrementViewCount должен увеличивать счетчик просмотров`() = runTest {
        // Given
        val paste = createTestPaste()
        pasteRepository.createPaste(paste)

        // When
        pasteRepository.incrementViewCount(paste.id)
        val updated = pasteRepository.getPasteById(paste.id)

        // Then
        assertNotNull(updated)
        assertEquals(1, updated.viewCount, "Счетчик должен увеличиться до 1")

        // Повторное увеличение
        pasteRepository.incrementViewCount(paste.id)
        val updatedAgain = pasteRepository.getPasteById(paste.id)
        assertNotNull(updatedAgain)
        assertEquals(2, updatedAgain.viewCount, "Счетчик должен увеличиться до 2")
    }

    @Test
    fun `getPublicPastes должен возвращать только публичные заметки`() = runTest {
        // Given
        val publicPaste1 = createTestPaste(id = "public1", visibility = PasteVisibility.PUBLIC)
        val publicPaste2 = createTestPaste(id = "public2", visibility = PasteVisibility.PUBLIC)
        val privatePaste = createTestPaste(id = "private1", visibility = PasteVisibility.PRIVATE)

        pasteRepository.createPaste(publicPaste1)
        pasteRepository.createPaste(publicPaste2)
        pasteRepository.createPaste(privatePaste)

        // When
        val publicPastes = pasteRepository.getPublicPastes(limit = 10, offset = 0)

        // Then
        assertEquals(2, publicPastes.size, "Должно быть найдено 2 публичные заметки")
        assertTrue(publicPastes.all { it.visibility == PasteVisibility.PUBLIC }, "Все заметки должны быть публичными")
        assertTrue(publicPastes.any { it.id == "public1" })
        assertTrue(publicPastes.any { it.id == "public2" })
        assertFalse(publicPastes.any { it.id == "private1" })
    }

    @Test
    fun `getPublicPastes должен поддерживать пагинацию`() = runTest {
        // Given - создаем 5 публичных заметок
        repeat(5) { i ->
            val paste = createTestPaste(id = "public$i", title = "Paste $i")
            pasteRepository.createPaste(paste)
        }

        // When - запрашиваем первые 3
        val firstPage = pasteRepository.getPublicPastes(limit = 3, offset = 0)
        val secondPage = pasteRepository.getPublicPastes(limit = 3, offset = 3)

        // Then
        assertEquals(3, firstPage.size, "Первая страница должна содержать 3 заметки")
        assertEquals(2, secondPage.size, "Вторая страница должна содержать 2 заметки")

        // Проверяем, что заметки не дублируются
        val allIds = (firstPage + secondPage).map { it.id }
        assertEquals(5, allIds.toSet().size, "Все ID должны быть уникальными")
    }

    @Test
    fun `getUserPastes должен возвращать заметки конкретного пользователя`() = runTest {
        // Given
        val user1Id = "user1"
        val user2Id = "user2"

        val user1Paste1 = createTestPaste(id = "u1p1", userId = user1Id, visibility = PasteVisibility.PUBLIC)
        val user1Paste2 = createTestPaste(id = "u1p2", userId = user1Id, visibility = PasteVisibility.PRIVATE)
        val user2Paste = createTestPaste(id = "u2p1", userId = user2Id, visibility = PasteVisibility.PUBLIC)
        val anonymousPaste = createTestPaste(id = "anon", userId = null)

        pasteRepository.createPaste(user1Paste1)
        pasteRepository.createPaste(user1Paste2)
        pasteRepository.createPaste(user2Paste)
        pasteRepository.createPaste(anonymousPaste)

        // When
        val user1Pastes = pasteRepository.getUserPastes(user1Id)

        // Then
        assertEquals(2, user1Pastes.size, "Пользователь 1 должен иметь 2 заметки")
        assertTrue(user1Pastes.all { it.userId == user1Id }, "Все заметки должны принадлежать пользователю 1")
        assertTrue(user1Pastes.any { it.id == "u1p1" })
        assertTrue(user1Pastes.any { it.id == "u1p2" })

        // Проверяем, что включены как публичные, так и приватные заметки
        assertTrue(user1Pastes.any { it.visibility == PasteVisibility.PUBLIC }, "Должны быть публичные заметки")
        assertTrue(user1Pastes.any { it.visibility == PasteVisibility.PRIVATE }, "Должны быть приватные заметки")
    }

    @Test
    fun `getUserPastes должен поддерживать пагинацию`() = runTest {
        // Given
        val userId = "paginated-user"
        repeat(7) { i ->
            val paste = createTestPaste(id = "page$i", userId = userId, title = "Paste $i")
            pasteRepository.createPaste(paste)
        }

        // When
        val firstPage = pasteRepository.getUserPastes(userId, limit = 3, offset = 0)
        val secondPage = pasteRepository.getUserPastes(userId, limit = 3, offset = 3)
        val thirdPage = pasteRepository.getUserPastes(userId, limit = 3, offset = 6)

        // Then
        assertEquals(3, firstPage.size, "Первая страница: 3 заметки")
        assertEquals(3, secondPage.size, "Вторая страница: 3 заметки")
        assertEquals(1, thirdPage.size, "Третья страница: 1 заметка")

        val allIds = (firstPage + secondPage + thirdPage).map { it.id }
        assertEquals(7, allIds.toSet().size, "Все ID должны быть уникальными")
    }

    @Test
    fun `getUserPastesCount должен возвращать правильное количество`() = runTest {
        // Given
        val userId = "count-user"
        repeat(5) { i ->
            val paste = createTestPaste(id = "count$i", userId = userId)
            pasteRepository.createPaste(paste)
        }

        // Создаем заметки другого пользователя
        repeat(3) { i ->
            val paste = createTestPaste(id = "other$i", userId = "other-user")
            pasteRepository.createPaste(paste)
        }

        // When
        val count = pasteRepository.getUserPastesCount(userId)

        // Then
        assertEquals(5, count, "Пользователь должен иметь 5 заметок")
    }

    @Test
    fun `deletePaste должен удалять заметку владельца`() = runTest {
        // Given
        val userId = "owner"
        val paste = createTestPaste(id = "deletable", userId = userId)
        pasteRepository.createPaste(paste)

        // When
        val deleted = pasteRepository.deletePaste("deletable", userId)

        // Then
        assertTrue(deleted, "Удаление должно быть успешным")

        val found = pasteRepository.getPasteById("deletable")
        assertNull(found, "Удаленная заметка не должна быть найдена")
    }

    @Test
    fun `deletePaste не должен удалять чужую заметку`() = runTest {
        // Given
        val ownerId = "owner"
        val otherId = "other"
        val paste = createTestPaste(id = "protected", userId = ownerId)
        pasteRepository.createPaste(paste)

        // When
        val deleted = pasteRepository.deletePaste("protected", otherId)

        // Then
        assertFalse(deleted, "Удаление чужой заметки должно быть неуспешным")

        val found = pasteRepository.getPasteById("protected")
        assertNotNull(found, "Заметка должна остаться в базе")
    }

    @Test
    fun `deletePaste не должен удалять анонимную заметку`() = runTest {
        // Given
        val paste = createTestPaste(id = "anonymous", userId = null)
        pasteRepository.createPaste(paste)

        // When
        val deleted = pasteRepository.deletePaste("anonymous", "someone")

        // Then
        assertFalse(deleted, "Удаление анонимной заметки должно быть неуспешным")

        val found = pasteRepository.getPasteById("anonymous")
        assertNotNull(found, "Анонимная заметка должна остаться в базе")
    }

    @Test
    fun `должен обрабатывать UTF-8 и кириллицу в заметках`() = runTest {
        // Given
        val paste = createTestPaste(
            id = "unicode",
            title = "Заголовок на русском 测试",
            content = "Содержимое с эмодзи 🚀 и разными языками: English, Русский, 中文",
            language = "текст"
        )

        // When
        val created = pasteRepository.createPaste(paste)
        val found = pasteRepository.getPasteById("unicode")

        // Then
        assertNotNull(created)
        assertNotNull(found)
        assertEquals(paste.title, found.title)
        assertEquals(paste.content, found.content)
        assertEquals(paste.language, found.language)
    }

    @Test
    fun `должен правильно сортировать заметки по дате создания`() = runTest {
        // Given - создаем заметки с небольшими задержками
        val paste1 = createTestPaste(id = "first", title = "First")
        pasteRepository.createPaste(paste1)

        Thread.sleep(100) // Небольшая задержка

        val paste2 = createTestPaste(id = "second", title = "Second")
        pasteRepository.createPaste(paste2)

        Thread.sleep(100)

        val paste3 = createTestPaste(id = "third", title = "Third")
        pasteRepository.createPaste(paste3)

        // When
        val publicPastes = pasteRepository.getPublicPastes(limit = 10, offset = 0)

        // Then
        assertEquals(3, publicPastes.size)
        // Новые заметки должны быть первыми (DESC order)
        assertEquals("third", publicPastes[0].id, "Самая новая заметка должна быть первой")
        assertEquals("second", publicPastes[1].id, "Вторая по новизне должна быть второй")
        assertEquals("first", publicPastes[2].id, "Самая старая должна быть последней")
    }
}
