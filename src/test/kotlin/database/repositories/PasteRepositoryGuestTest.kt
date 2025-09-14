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

class PasteRepositoryGuestTest {

    private lateinit var pasteRepository: PasteRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:paste_guest_test;DB_CLOSE_DELAY=-1;",
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

    private fun guestPaste(
        id: String = "guestpst1234",
        title: String = "Guest Paste",
        content: String = "Guest content",
        guestId: String = "guest-1",
        visibility: PasteVisibility = PasteVisibility.PUBLIC
    ): Paste {
        val now = LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = title,
            content = content,
            userId = null,
            visibility = visibility,
            createdAt = now,
            updatedAt = now,
            expiresAt = null,
            syntaxLanguage = "plaintext",
            viewCount = 0,
            guestId = guestId
        )
    }

    @Test
    fun `create and list guest pastes`() = runTest {
        val p1 = guestPaste(id = "guest000001", guestId = "gX")
        pasteRepository.createPaste(p1)

        val result = pasteRepository.getGuestPastes("gX", null, limit = 10, offset = 0, sortOrder = PasteSortOrder.NEWEST_FIRST)
        assertEquals(1, result.size)
        assertEquals("guest000001", result[0].id)
        assertNull(result[0].userId)
        assertEquals("gX", result[0].guestId)
    }

    @Test
    fun `deletePasteByGuest removes owned paste`() = runTest {
        val p = guestPaste(id = "guest000003", guestId = "gDel")
        pasteRepository.createPaste(p)

        val deleted = pasteRepository.deletePasteByGuest("guest000003", "gDel")
        assertTrue(deleted)
        val found = pasteRepository.getPasteById("guest000003")
        assertNull(found)
    }
}
