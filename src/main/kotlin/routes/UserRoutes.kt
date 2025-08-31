package tech.nimbus.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.nimbus.database.repositories.UserRepository
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.shared.dto.request.UpdateProfileRequestDto
import tech.nimbus.shared.dto.UserProfileDto
import tech.nimbus.shared.dto.ApiErrorDto
import tech.nimbus.utils.DtoConverters.toUserDto
import tech.nimbus.utils.DtoConverters.toPasteDtoWithAuthor

fun Route.userRoutes(userRepo: UserRepository, pasteRepo: PasteRepository) {
    route("/api/users") {

        // GET /api/users/{id} - получить профиль пользователя по ID
        get("/{id}") {
            try {
                val userId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiErrorDto("Missing user ID")
                )

                val user = userRepo.getUserById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ApiErrorDto("User not found"))
                    return@get
                }

                // Получаем количество публичных заметок пользователя
                val publicPastesCount = pasteRepo.getUserPastesCount(
                    userId,
                    tech.nimbus.models.PasteVisibility.PUBLIC
                ).toInt()

                val userProfile = UserProfileDto(
                    user = user.toUserDto(),
                    publicPastesCount = publicPastesCount,
                    totalPastesCount = null  // Не показываем общее количество для чужих профилей
                )

                call.respond(HttpStatusCode.OK, userProfile)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiErrorDto("${e.message}"))
            }
        }

        // GET /api/users/{id}/pastes - получить публичные заметки пользователя
        get("/{id}/pastes") {
            try {
                val userId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiErrorDto("Missing user ID")
                )

                // Проверяем, что пользователь существует
                val user = userRepo.getUserById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ApiErrorDto("User not found"))
                    return@get
                }

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = (page - 1) * limit

                // Получаем публичные заметки пользователя с информацией об авторе
                val pastesWithAuthors = pasteRepo.getUserPublicPastes(userId, limit, offset)
                val pasteResponses = pastesWithAuthors.map { (paste, author) ->
                    paste.toPasteDtoWithAuthor(author)
                }

                call.respond(HttpStatusCode.OK, pasteResponses)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiErrorDto("${e.message}"))
            }
        }

        // PUT /api/users/profile - обновить свой профиль (требует авторизации)
        authenticate("auth-jwt") {
            put("/profile") {
                try {
                    val req = call.receive<UpdateProfileRequestDto>()
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("userId").asString()

                    // Проверяем, что хотя бы одно поле для обновления указано
                    if (req.username == null && req.displayName == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiErrorDto("No fields to update"))
                        return@put
                    }

                    // Проверяем уникальность username, если он указан
                    req.username?.let { newUsername ->
                        val existingUser = userRepo.findUserByUsername(newUsername)
                        if (existingUser != null && existingUser.id != userId) {
                            call.respond(HttpStatusCode.Conflict, ApiErrorDto("Username already exists"))
                            return@put
                        }
                    }

                    // Обновляем профиль
                    val updated = userRepo.updateUserProfile(
                        userId = userId,
                        username = req.username,
                        displayName = req.displayName
                    )

                    if (!updated) {
                        call.respond(HttpStatusCode.InternalServerError, ApiErrorDto("Failed to update profile"))
                        return@put
                    }

                    // Возвращаем обновленные данные пользователя
                    val updatedUser = userRepo.getUserById(userId)
                        ?: return@put call.respond(HttpStatusCode.InternalServerError, ApiErrorDto("Failed to retrieve updated user"))

                    call.respond(HttpStatusCode.OK, updatedUser.toUserDto())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiErrorDto("Invalid request: ${e.message}"))
                }
            }

            // GET /api/users/profile - получить свой профиль с полной статистикой
            get("/profile") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("userId").asString()

                    val user = userRepo.getUserById(userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ApiErrorDto("User not found"))

                    // Получаем статистику для владельца профиля
                    val publicPastesCount = pasteRepo.getUserPastesCount(
                        userId,
                        tech.nimbus.models.PasteVisibility.PUBLIC
                    ).toInt()

                    val totalPastesCount = pasteRepo.getUserPastesCount(userId).toInt()

                    val userProfile = UserProfileDto(
                        user = user.toUserDto(),
                        publicPastesCount = publicPastesCount,
                        totalPastesCount = totalPastesCount  // Показываем полную статистику владельцу
                    )

                    call.respond(HttpStatusCode.OK, userProfile)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiErrorDto("${e.message}"))
                }
            }
        }
    }
}
