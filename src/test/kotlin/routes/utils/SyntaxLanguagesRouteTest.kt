package routes.utils

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tech.nimbus.routes.utils.syntaxLanguagesRoute

class SyntaxLanguagesRouteTest {
    @Test
    fun `GET syntax-languages returns list of languages`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { syntaxLanguagesRoute() }
        }
        val response = client.get("/api/utils/syntax-languages")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("plaintext"), "Response should contain 'plaintext'")
        assertTrue(body.contains("kotlin"), "Response should contain 'kotlin'")
    }
}
