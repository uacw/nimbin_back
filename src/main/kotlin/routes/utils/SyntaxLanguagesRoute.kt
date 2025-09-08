package tech.nimbus.routes.utils

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.syntaxLanguagesRoute() {
    get("/api/utils/syntax-languages") {
        val languages = listOf(
            "plaintext", "c", "cpp", "csharp", "css", "go", "html", "java",
            "javascript", "json", "kotlin", "lua", "markdown", "php", "python",
            "ruby", "rust", "sql", "swift", "typescript", "xml", "yaml"
        )
        call.respond(languages)
    }
}