package tech.nimbus.database.repositories

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.*
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.utils.EtagUtil
import java.time.LocalDateTime

@DisplayName("PasteRepository ETag/Update Tests")
class PasteRepositoryEtagTest {

    private lateinit var repo: PasteRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:paste_etag_test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        repo = PasteRepository()
        transaction(database) {
            SchemaUtils.create(UserTable, PasteTable)
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(PasteTable, UserTable)
        }
    }

    private fun newPaste(
        id: String = "etagp1234567",
        title: String = "Title",
        content: String = "Content",
        userId: String? = "user1",
        visibility: PasteVisibility = PasteVisibility.PUBLIC,
        syntaxLanguage: String = "plaintext"
    ): Paste {
        val now = LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = title,
            content = content,
            userId = userId,
            visibility = visibility,
            createdAt = now,
            updatedAt = now,
            expiresAt = null,
            syntaxLanguage = syntaxLanguage,
            viewCount = 0
        )
    }

    @Test
    fun `update should succeed when ETag matches`() = runTest {
        // Arrange
        val original = newPaste(id = "etagok123456")
        val created = repo.createPaste(original)!!
        val current = repo.getPasteById(created.id)!!
        val etag = EtagUtil.compute(current.content, current.updatedAt)

        // Act
        val updated = repo.updatePaste(
            pasteId = created.id,
            title = "New Title",
            content = current.content,
            syntaxLanguage = current.syntaxLanguage,
            visibility = current.visibility,
            expiresAt = current.expiresAt,
            expectedEtag = etag
        )

        // Assert
        assertNotNull(updated, "Update must succeed when ETag matches")
        assertEquals("New Title", updated.title)
        assertNotEquals(current.updatedAt, updated.updatedAt, "updatedAt must change on update")
        // ETag must change since updatedAt changed
        val newEtag = EtagUtil.compute(updated.content, updated.updatedAt)
        assertNotEquals(etag, newEtag, "ETag must be refreshed after update")
    }

    @Test
    fun `update should return null when ETag mismatches`() = runTest {
        // Arrange
        val original = newPaste(id = "etagno123456")
        val created = repo.createPaste(original)!!
        val wrongEtag = "deadbeef" // definitely wrong

        // Act
        val result = repo.updatePaste(
            pasteId = created.id,
            title = "Hacked",
            expectedEtag = wrongEtag
        )

        // Assert
        assertNull(result, "Update must be blocked on ETag mismatch")
        val still = repo.getPasteById(created.id)!!
        assertEquals(original.title, still.title, "Title must remain unchanged on conflict")
    }

    @Test
    fun `update should return null when paste not found`() = runTest {
        // Act
        val result = repo.updatePaste(
            pasteId = "notexist0000",
            title = "Nope",
            expectedEtag = null
        )

        // Assert
        assertNull(result, "Update must return null for missing paste")
    }
}
