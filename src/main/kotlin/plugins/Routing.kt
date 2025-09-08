package tech.nimbus.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import tech.nimbus.routes.authRoutes
import tech.nimbus.routes.pasteRoutes
import tech.nimbus.routes.userRoutes
import tech.nimbus.services.JwtService
import tech.nimbus.database.repositories.UserRepository
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.routes.utils.syntaxLanguagesRoute

fun Application.configureRouting() {
    // Получаем переменные окружения для Heroku
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: "your-secret-key-for-jwt-tokens-make-it-long-and-secure" // fallback для локальной разработки
    val jwtIssuer = System.getenv("JWT_ISSUER")
        ?: "tech.nimbus"
    val jwtAudience = System.getenv("JWT_AUDIENCE")
        ?: "tech.nimbus.audience"

    // Инициализация сервисов с переменными окружения
    val jwtService = JwtService(
        secret = jwtSecret,
        issuer = jwtIssuer,
        audience = jwtAudience
    )
    val userRepo = UserRepository()
    val pasteRepo = PasteRepository()

    routing {
        pasteRoutes()
        authRoutes(jwtService, userRepo)
        userRoutes(userRepo, pasteRepo)
        // healthRoutes() и т.д.
        syntaxLanguagesRoute()
    }
}
