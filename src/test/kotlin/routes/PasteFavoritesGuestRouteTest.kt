package tech.nimbus.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserFavoritesTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.services.JwtService
import tech.nimbus.shared.dto.PasteDto
import tech.nimbus.routes.utils.syntaxLanguagesRoute
import tech.nimbus.plugins.configureSecurity

class PasteFavoritesGuestRouteTest {

    private fun newPaste(id: String, visibility: PasteVisibility = PasteVisibility.PUBLIC, guestId: String? = null): Paste {
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
    fun `guest favorites basic flow`() = testApplication {
        val url = "jdbc:h2:mem:guest_fav_basic;DB_CLOSE_DELAY=-1;"
        val db = Database.connect(url, driver = "org.h2.Driver")
        transaction(db) { SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable) }

        application {
            // Подключаем тот же H2 для потоков Ktor
            Database.connect(url, driver = "org.h2.Driver")
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing {
                pasteRoutes()
                syntaxLanguagesRoute()
            }
        }

        val jwt = JwtService(
            secret = "your-secret-key-for-jwt-tokens-make-it-long-and-secure",
            issuer = "tech.nimbus",
            audience = "tech.nimbus.audience"
        )
        val guestId = "guest-basic-1"
        val guestToken = jwt.generateGuestToken(guestId, ttlMillis = 24 * 3600_000)

        // Create a paste
        val repo = PasteRepository()
        val p = newPaste("pFavA001234", PasteVisibility.PUBLIC)
        repo.createPaste(p)

        // Act: add to favorites (guest)
        val addResp = client.post("/api/pastes/${p.id}/favorite") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        assertEquals(HttpStatusCode.OK, addResp.status)

        // Assert: GET /my?favorite=true contains this paste
        val listResp = client.get("/api/pastes/my?favorite=true") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        assertEquals(HttpStatusCode.OK, listResp.status)
        val body = listResp.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val items = json.decodeFromString(ListSerializer(PasteDto.serializer()), body)
        assertTrue(items.any { it.id == p.id }, "Guest favorites list should contain the added paste")

        // Remove favorite
        val delResp = client.delete("/api/pastes/${p.id}/favorite") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        assertEquals(HttpStatusCode.OK, delResp.status)

        val listResp2 = client.get("/api/pastes/my?favorite=true") {
            header(HttpHeaders.Authorization, "Bearer $guestToken")
        }
        val body2 = listResp2.bodyAsText()
        val items2 = json.decodeFromString(ListSerializer(PasteDto.serializer()), body2)
        assertTrue(items2.none { it.id == p.id }, "Guest favorites list should be empty after removal")
    }

    @Test
    fun `guest favorites isolation between guests`() = testApplication {
        val url = "jdbc:h2:mem:guest_fav_isolation;DB_CLOSE_DELAY=-1;"
        val db = Database.connect(url, driver = "org.h2.Driver")
        transaction(db) { SchemaUtils.create(UserTable, PasteTable, UserFavoritesTable) }

        application {
            Database.connect(url, driver = "org.h2.Driver")
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing { pasteRoutes() }
        }

        val jwt = JwtService(
            secret = "your-secret-key-for-jwt-tokens-make-it-long-and-secure",
            issuer = "tech.nimbus",
            audience = "tech.nimbus.audience"
        )
        val guestA = jwt.generateGuestToken("guest-A", ttlMillis = 24 * 3600_000)
        val guestB = jwt.generateGuestToken("guest-B", ttlMillis = 24 * 3600_000)

        val repo = PasteRepository()
        val p1 = newPaste("pIso00000111", PasteVisibility.PUBLIC)
        val p2 = newPaste("pIso00000222", PasteVisibility.PUBLIC)
        repo.createPaste(p1)
        repo.createPaste(p2)

        // A -> p1
        client.post("/api/pastes/${p1.id}/favorite") { header(HttpHeaders.Authorization, "Bearer $guestA") }
        // B -> p2
        client.post("/api/pastes/${p2.id}/favorite") { header(HttpHeaders.Authorization, "Bearer $guestB") }

        val json = Json { ignoreUnknownKeys = true }

        val listA = client.get("/api/pastes/my?favorite=true") { header(HttpHeaders.Authorization, "Bearer $guestA") }
        val bodyA = listA.bodyAsText()
        val itemsA = json.decodeFromString(ListSerializer(PasteDto.serializer()), bodyA)
        assertTrue(itemsA.any { it.id == p1.id } && itemsA.none { it.id == p2.id })

        val listB = client.get("/api/pastes/my?favorite=true") { header(HttpHeaders.Authorization, "Bearer $guestB") }
        val bodyB = listB.bodyAsText()
        val itemsB = json.decodeFromString(ListSerializer(PasteDto.serializer()), bodyB)
        assertTrue(itemsB.any { it.id == p2.id } && itemsB.none { it.id == p1.id })
    }
}
