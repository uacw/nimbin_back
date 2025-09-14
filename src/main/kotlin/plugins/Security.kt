package tech.nimbus.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    // Загружаем те же значения, что используются при генерации токена (см. Routing.kt)
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: "your-secret-key-for-jwt-tokens-make-it-long-and-secure"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "tech.nimbus"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "tech.nimbus.audience"
    val jwtRealm = System.getenv("JWT_REALM") ?: "Access to Nimbus API"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val payload = credential.payload
                val userId = payload.getClaim("userId").asString()
                val guestId = payload.getClaim("guestId").asString()
                val isGuest = runCatching { payload.getClaim("isGuest").asBoolean() }.getOrNull() ?: false

                when {
                    userId != null && !isGuest -> JWTPrincipal(payload)
                    guestId != null && isGuest -> JWTPrincipal(payload)
                    else -> null
                }
            }
            challenge { _, _ ->
                call.respond(
                    status = io.ktor.http.HttpStatusCode.Unauthorized,
                    message = mapOf("error" to "Token is not valid or has expired")
                )
            }
        }
    }
}
