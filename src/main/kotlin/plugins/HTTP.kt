package tech.nimbus.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.event.Level

fun Application.configureHTTP() {
    install(CallLogging) { level = Level.INFO }         // CallLogging[2]

    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AcceptCharset)
        allowCredentials = true
        anyHost()
    }

    install(StatusPages) {                              // StatusPages[3]
        exception<Throwable> { call, cause ->
            call.respondText(
                text = """{"error": "${cause.message ?: "unknown error"}"}""",
                contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                status = HttpStatusCode.InternalServerError
            )
            throw cause
        }
    }
}