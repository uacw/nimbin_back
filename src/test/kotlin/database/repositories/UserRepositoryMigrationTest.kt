package tech.nimbus.database.repositories

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserFavoritesTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest

class UserRepositoryMigrationTest {

    private lateinit var db: Database
    private lateinit var userRepo: UserRepository
    private lateinit var favRepo: FavoritesRepository
    private lateinit var pasteRepo: PasteRepository

    @BeforeTest
    fun setup() {
        db = Database.connect("jdbc:h2:mem:user_migration_test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        userRepo = UserRepository()
        favRepo = FavoritesRepository()
        pasteRepo = PasteRepository()
        transaction(db) { SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable) }
    }

    private fun newPaste(id: String): Paste {
        val now = LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = "t-$id",
            content = "c-$id",
            userId = null,
            guestId = null,
            visibility = PasteVisibility.PUBLIC,
            createdAt = now,
            updatedAt = now,
            expiresAt = null,
            syntaxLanguage = "plaintext",
            viewCount = 0
        )
    }

    @Test
    fun `migration handles favorite conflict without duplicates`() = runTest {
        val guestId = "g-conflict-1"
        val userId = "user-A"
        val pid = "p-1234abcdEFG".take(12)
        pasteRepo.createPaste(newPaste(pid))
        // Уже есть у пользователя A
        favRepo.addFavorite(userId, pid)
        // Гость тоже добавляет тот же paste
        favRepo.addFavoriteGuest(guestId, pid)

        val affected = userRepo.migrateGuestData(guestId, userId)
        assertTrue(affected >= 1)

        val userIds = favRepo.listFavoritePasteIds(userId)
        assertEquals(listOf(pid), userIds)
        val guestIds = favRepo.listFavoritePasteIdsGuest(guestId)
        assertTrue(guestIds.isEmpty())
    }

    @Test
    fun `migration is idempotent`() = runTest {
        val guestId = "g-idem-1"
        val userId = "user-B"
        val pid1 = "p-idem0000011".take(12)
        val pid2 = "p-idem0000022".take(12)
        pasteRepo.createPaste(newPaste(pid1))
        pasteRepo.createPaste(newPaste(pid2))
        favRepo.addFavoriteGuest(guestId, pid1)
        favRepo.addFavoriteGuest(guestId, pid2)

        val a1 = userRepo.migrateGuestData(guestId, userId)
        val a2 = userRepo.migrateGuestData(guestId, userId)
        assertTrue(a1 >= 1)
        assertTrue(a2 >= 0)

        val userFavs = favRepo.listFavoritePasteIds(userId).sorted()
        assertEquals(listOf(pid1, pid2).sorted(), userFavs)
        val guestFavs = favRepo.listFavoritePasteIdsGuest(guestId)
        assertTrue(guestFavs.isEmpty())
    }
}
