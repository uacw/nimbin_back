package tech.nimbus.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import tech.nimbus.database.repositories.FavoritesRepository
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.database.repositories.UserRepository
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserFavoritesTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.services.JwtService
import tech.nimbus.shared.dto.AuthResponseDto
import tech.nimbus.shared.dto.request.RegisterRequestDto
import kotlinx.serialization.encodeToString
import tech.nimbus.plugins.configureSecurity

class AuthRegisterMigrationTest {

    private fun newPaste(id: String, visibility: PasteVisibility, guestId: String): Paste {
        val now = java.time.LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = "t-$id",
            content = "c-$id",
            userId = null,
            guestId = guestId,
            visibility = visibility,
            createdAt = now,
            updatedAt = now,
            expiresAt = null,
            syntaxLanguage = "plaintext",
            viewCount = 0
        )
    }

    @Test
    fun `guest to user migration happy path`() = testApplication {
        val url = "jdbc:h2:mem:migration_happy;DB_CLOSE_DELAY=-1;"
        val db = Database.connect(url, driver = "org.h2.Driver")
        transaction(db) { SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable) }

        val jwt = JwtService(
            secret = "your-secret-key-for-jwt-tokens-make-it-long-and-secure",
            issuer = "tech.nimbus",
            audience = "tech.nimbus.audience"
        )
        val guestId = "guest-happy-1"
        val guestToken = jwt.generateGuestToken(guestId, ttlMillis = 24 * 3600_000)

        val pasteRepo = PasteRepository()
        val favRepo = FavoritesRepository()

        val pPub = newPaste("pHappy000111", PasteVisibility.PUBLIC, guestId)
        val pUnl = newPaste("pHappy000222", PasteVisibility.UNLISTED, guestId)
        val pPriv = newPaste("pHappy000333", PasteVisibility.PRIVATE, guestId)
        // Вызываем suspend-методы напрямую (они сами открывают transaction внутри)
        pasteRepo.createPaste(pPub)
        pasteRepo.createPaste(pUnl)
        pasteRepo.createPaste(pPriv)
        favRepo.addFavoriteGuest(guestId, pPub.id)
        favRepo.addFavoriteGuest(guestId, pUnl.id)

        application {
            // Подключаем ту же БД для потоков Ktor
            Database.connect(url, driver = "org.h2.Driver")
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                val userRepo = UserRepository()
                authRoutes(jwt, userRepo)
            }
        }

        val body = Json.encodeToString(RegisterRequestDto(username = "happyUser", email = "happy@example.com", password = "Passw0rd!"))
        val resp = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, resp.status)

        val json = Json { ignoreUnknownKeys = true }
        val auth = json.decodeFromString(AuthResponseDto.serializer(), resp.bodyAsText())
        assertTrue(auth.token.isNotBlank())
        val newUserId = auth.user.id

        // Assert DB state (репозитории уже управляют транзакциями)
        val pub = pasteRepo.getPasteById(pPub.id)!!
        val unl = pasteRepo.getPasteById(pUnl.id)!!
        val prv = pasteRepo.getPasteById(pPriv.id)!!
        listOf(pub, unl, prv).forEach {
            assertEquals(newUserId, it.userId)
            assertNull(it.guestId)
        }
        // favorites migrated
        val ids = favRepo.listFavoritePasteIds(newUserId)
        assertTrue(ids.containsAll(listOf(pPub.id, pUnl.id)))
        // guest favorites cleared
        val guestFavIds = favRepo.listFavoritePasteIdsGuest(guestId)
        assertTrue(guestFavIds.isEmpty())
    }

    @Test
    fun `guest to user migration with empty guest`() = testApplication {
        val url = "jdbc:h2:mem:migration_empty;DB_CLOSE_DELAY=-1;"
        val db = Database.connect(url, driver = "org.h2.Driver")
        transaction(db) { SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable) }

        val jwt = JwtService(
            secret = "your-secret-key-for-jwt-tokens-make-it-long-and-secure",
            issuer = "tech.nimbus",
            audience = "tech.nimbus.audience"
        )
        val guestId = "guest-empty-1"
        val guestToken = jwt.generateGuestToken(guestId, ttlMillis = 24 * 3600_000)

        application {
            Database.connect(url, driver = "org.h2.Driver")
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                val userRepo = UserRepository()
                authRoutes(jwt, userRepo)
            }
        }

        val body = Json.encodeToString(RegisterRequestDto(username = "emptyUser", email = "empty@example.com", password = "Passw0rd!"))
        val resp = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val auth = Json { ignoreUnknownKeys = true }.decodeFromString(AuthResponseDto.serializer(), resp.bodyAsText())
        assertTrue(auth.token.isNotBlank())
    }
}
