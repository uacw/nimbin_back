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
 * Unit тесты для новой функциональности трех типов видимости заметок.
 */
class PasteVisibilityRepositoryTest {

    private lateinit var pasteRepository: PasteRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:visibility_test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        pasteRepository = PasteRepository()

        transaction(database) {
            SchemaUtils.create(UserTable, PasteTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction(database) {
            SchemaUtils.drop(PasteTable, UserTable)
        }
    }

    private fun createTestPaste(
        id: String,
        visibility: PasteVisibility,
        userId: String? = "user123"
    ): Paste {
        return Paste(
            id = id,
            title = "Test Paste $id",
            content = "Content for $id",
            userId = userId,
            visibility = visibility,
            createdAt = LocalDateTime.now().toString(),
            expiresAt = null,
            language = "text",
            viewCount = 0
        )
    }

    @Test
    fun `getPublicPastes должен возвращать только PUBLIC заметки`() = runTest {
        // Given
        val publicPaste = createTestPaste("public1", PasteVisibility.PUBLIC)
        val unlistedPaste = createTestPaste("unlisted1", PasteVisibility.UNLISTED)
        val privatePaste = createTestPaste("private1", PasteVisibility.PRIVATE)

        pasteRepository.createPaste(publicPaste)
        pasteRepository.createPaste(unlistedPaste)
        pasteRepository.createPaste(privatePaste)

        // When
        val publicPastes = pasteRepository.getPublicPastes(limit = 10, offset = 0)

        // Then
        assertEquals(1, publicPastes.size, "Должна быть найдена только 1 публичная заметка")
        assertEquals("public1", publicPastes[0].id)
        assertEquals(PasteVisibility.PUBLIC, publicPastes[0].visibility)
    }

    @Test
    fun `getPublicPastesCount должен считать только PUBLIC заметки`() = runTest {
        // Given
        repeat(3) { i ->
            pasteRepository.createPaste(createTestPaste("public$i", PasteVisibility.PUBLIC))
        }
        repeat(2) { i ->
            pasteRepository.createPaste(createTestPaste("unlisted$i", PasteVisibility.UNLISTED))
        }
        repeat(1) { i ->
            pasteRepository.createPaste(createTestPaste("private$i", PasteVisibility.PRIVATE))
        }

        // When
        val count = pasteRepository.getPublicPastesCount()

        // Then
        assertEquals(3, count, "Должно быть подсчитано только 3 публичные заметки")
    }

    @Test
    fun `canAccessPaste должен разрешать доступ к PUBLIC заметкам всем пользователям`() = runTest {
        // Given
        val publicPaste = createTestPaste("public1", PasteVisibility.PUBLIC, "owner")
        pasteRepository.createPaste(publicPaste)

        // When & Then
        assertTrue(
            pasteRepository.canAccessPaste("public1", "owner"),
            "Владелец должен иметь доступ к своей публичной заметке"
        )
        assertTrue(
            pasteRepository.canAccessPaste("public1", "other-user"),
            "Другой пользователь должен иметь доступ к публичной заметке"
        )
        assertTrue(
            pasteRepository.canAccessPaste("public1", null),
            "Анонимный пользователь должен иметь доступ к публичной заметке"
        )
    }

    @Test
    fun `canAccessPaste должен разрешать доступ к UNLISTED заметкам всем пользователям`() = runTest {
        // Given
        val unlistedPaste = createTestPaste("unlisted1", PasteVisibility.UNLISTED, "owner")
        pasteRepository.createPaste(unlistedPaste)

        // When & Then
        assertTrue(
            pasteRepository.canAccessPaste("unlisted1", "owner"),
            "Владелец должен иметь доступ к своей скрытой заметке"
        )
        assertTrue(
            pasteRepository.canAccessPaste("unlisted1", "other-user"),
            "Другой пользователь должен иметь доступ к скрытой заметке при знании ID"
        )
        assertTrue(
            pasteRepository.canAccessPaste("unlisted1", null),
            "Анонимный пользователь должен иметь доступ к скрытой заметке при знании ID"
        )
    }

    @Test
    fun `canAccessPaste должен разрешать доступ к PRIVATE заметкам только владельцу`() = runTest {
        // Given
        val privatePaste = createTestPaste("private1", PasteVisibility.PRIVATE, "owner")
        pasteRepository.createPaste(privatePaste)

        // When & Then
        assertTrue(
            pasteRepository.canAccessPaste("private1", "owner"),
            "Владелец должен иметь доступ к своей приватной заметке"
        )
        assertFalse(
            pasteRepository.canAccessPaste("private1", "other-user"),
            "Другой пользователь НЕ должен иметь доступ к приватной заметке"
        )
        assertFalse(
            pasteRepository.canAccessPaste("private1", null),
            "Анонимный пользователь НЕ должен иметь доступ к приватной заметке"
        )
    }

    @Test
    fun `canAccessPaste должен возвращать false для несуществующих заметок`() = runTest {
        // When & Then
        assertFalse(
            pasteRepository.canAccessPaste("nonexistent", "anyone"),
            "Несуществующая заметка должна возвращать false"
        )
    }

    @Test
    fun `getUserPastes должен возвращать заметки всех типов видимости для владельца`() = runTest {
        // Given
        val userId = "user123"
        val publicPaste = createTestPaste("public1", PasteVisibility.PUBLIC, userId)
        val unlistedPaste = createTestPaste("unlisted1", PasteVisibility.UNLISTED, userId)
        val privatePaste = createTestPaste("private1", PasteVisibility.PRIVATE, userId)

        pasteRepository.createPaste(publicPaste)
        pasteRepository.createPaste(unlistedPaste)
        pasteRepository.createPaste(privatePaste)

        // When
        val userPastes = pasteRepository.getUserPastes(userId)

        // Then
        assertEquals(3, userPastes.size, "Пользователь должен видеть все свои заметки")

        val visibilityTypes = userPastes.map { it.visibility }.toSet()
        assertTrue(visibilityTypes.contains(PasteVisibility.PUBLIC), "Должны быть PUBLIC заметки")
        assertTrue(visibilityTypes.contains(PasteVisibility.UNLISTED), "Должны быть UNLISTED заметки")
        assertTrue(visibilityTypes.contains(PasteVisibility.PRIVATE), "Должны быть PRIVATE заметки")
    }

    @Test
    fun `должен правильно создавать заметки всех типов видимости`() = runTest {
        // Given & When
        val publicPaste = createTestPaste("public1", PasteVisibility.PUBLIC)
        val unlistedPaste = createTestPaste("unlisted1", PasteVisibility.UNLISTED)
        val privatePaste = createTestPaste("private1", PasteVisibility.PRIVATE)

        val createdPublic = pasteRepository.createPaste(publicPaste)
        val createdUnlisted = pasteRepository.createPaste(unlistedPaste)
        val createdPrivate = pasteRepository.createPaste(privatePaste)

        // Then
        assertNotNull(createdPublic)
        assertNotNull(createdUnlisted)
        assertNotNull(createdPrivate)

        assertEquals(PasteVisibility.PUBLIC, createdPublic.visibility)
        assertEquals(PasteVisibility.UNLISTED, createdUnlisted.visibility)
        assertEquals(PasteVisibility.PRIVATE, createdPrivate.visibility)
    }

    @Test
    fun `должен правильно извлекать заметки всех типов видимости`() = runTest {
        // Given
        val publicPaste = createTestPaste("public1", PasteVisibility.PUBLIC)
        val unlistedPaste = createTestPaste("unlisted1", PasteVisibility.UNLISTED)
        val privatePaste = createTestPaste("private1", PasteVisibility.PRIVATE)

        pasteRepository.createPaste(publicPaste)
        pasteRepository.createPaste(unlistedPaste)
        pasteRepository.createPaste(privatePaste)

        // When
        val foundPublic = pasteRepository.getPasteById("public1")
        val foundUnlisted = pasteRepository.getPasteById("unlisted1")
        val foundPrivate = pasteRepository.getPasteById("private1")

        // Then
        assertNotNull(foundPublic)
        assertNotNull(foundUnlisted)
        assertNotNull(foundPrivate)

        assertEquals(PasteVisibility.PUBLIC, foundPublic.visibility)
        assertEquals(PasteVisibility.UNLISTED, foundUnlisted.visibility)
        assertEquals(PasteVisibility.PRIVATE, foundPrivate.visibility)
    }

    @Test
    fun `анонимные заметки должны поддерживать все типы видимости кроме PRIVATE`() = runTest {
        // Given
        val publicAnonymous = createTestPaste("anon-pub", PasteVisibility.PUBLIC, null)
        val unlistedAnonymous = createTestPaste("anon-unlist", PasteVisibility.UNLISTED, null)

        // When
        val createdPublic = pasteRepository.createPaste(publicAnonymous)
        val createdUnlisted = pasteRepository.createPaste(unlistedAnonymous)

        // Then
        assertNotNull(createdPublic)
        assertNotNull(createdUnlisted)
        assertNull(createdPublic.userId)
        assertNull(createdUnlisted.userId)
        assertEquals(PasteVisibility.PUBLIC, createdPublic.visibility)
        assertEquals(PasteVisibility.UNLISTED, createdUnlisted.visibility)
    }

    @Test
    fun `смешанные сценарии доступа к заметкам разных типов видимости`() = runTest {
        // Given
        val owner = "owner"
        val other = "other"

        pasteRepository.createPaste(createTestPaste("pub1", PasteVisibility.PUBLIC, owner))
        pasteRepository.createPaste(createTestPaste("unl1", PasteVisibility.UNLISTED, owner))
        pasteRepository.createPaste(createTestPaste("prv1", PasteVisibility.PRIVATE, owner))
        pasteRepository.createPaste(createTestPaste("pub2", PasteVisibility.PUBLIC, other))

        // When - проверяем доступ владельца ко всем своим заметкам
        val ownerAccess = listOf("pub1", "unl1", "prv1").map { id ->
            pasteRepository.canAccessPaste(id, owner)
        }

        // When - проверяем доступ другого пользователя
        val otherAccess = listOf("pub1", "unl1", "prv1").map { id ->
            pasteRepository.canAccessPaste(id, other)
        }

        // When - проверяем доступ анонимного пользователя
        val anonAccess = listOf("pub1", "unl1", "prv1").map { id ->
            pasteRepository.canAccessPaste(id, null)
        }

        // Then
        assertTrue(ownerAccess.all { it }, "Владелец должен иметь доступ ко всем своим заметкам")
        assertEquals(listOf(true, true, false), otherAccess, "Другой пользователь: доступ к PUBLIC и UNLISTED, но не к PRIVATE")
        assertEquals(listOf(true, true, false), anonAccess, "Анонимный пользователь: доступ к PUBLIC и UNLISTED, но не к PRIVATE")
    }
}
