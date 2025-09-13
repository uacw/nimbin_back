package tech.nimbus.database.repositories

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*
import tech.nimbus.database.tables.UserFavoritesTable
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import java.time.LocalDateTime

class FavoritesRepositoryTest {

    private lateinit var db: Database
    private lateinit var favorites: FavoritesRepository
    private lateinit var pastes: PasteRepository

    @BeforeEach
    fun setup() {
        db = Database.connect(
            url = "jdbc:h2:mem:favorites_test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        favorites = FavoritesRepository()
        pastes = PasteRepository()

        transaction(db) {
            SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction(db) {
            SchemaUtils.drop(UserFavoritesTable, PasteTable, UserTable)
        }
    }

    private fun newPaste(id: String = "p1", userId: String? = "u1", visibility: PasteVisibility = PasteVisibility.PUBLIC): Paste {
        val now = LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = "t-$id",
            content = "c-$id",
            userId = userId,
            visibility = visibility,
            createdAt = now,
            updatedAt = now,
            expiresAt = null,
            syntaxLanguage = "plaintext",
            viewCount = 0
        )
    }

    @Test
    fun `add and remove favorite works`() = runTest {
        // Arrange
        val paste = newPaste("p1")
        pastes.createPaste(paste)

        // Act
        val added = favorites.addFavorite("u1", "p1")

        // Assert
        assertTrue(added)
        assertTrue(favorites.isFavorite("u1", "p1"))

        val ids = favorites.listFavoritePasteIds("u1")
        assertEquals(listOf("p1"), ids)

        // Remove
        val removed = favorites.removeFavorite("u1", "p1")
        assertTrue(removed)
        assertFalse(favorites.isFavorite("u1", "p1"))
        assertTrue(favorites.listFavoritePasteIds("u1").isEmpty())
    }

    @Test
    fun `second addFavorite is idempotent`() = runTest {
        val paste = newPaste("p2")
        pastes.createPaste(paste)

        val first = favorites.addFavorite("u1", "p2")
        val second = favorites.addFavorite("u1", "p2")

        assertTrue(first)
        // insertIgnore может вернуть 0 при повторе
        assertFalse(second)
        assertEquals(listOf("p2"), favorites.listFavoritePasteIds("u1"))
    }
}

