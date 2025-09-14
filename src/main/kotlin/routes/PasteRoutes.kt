package tech.nimbus.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.nimbus.routes.utils.getCurrentUserId
import tech.nimbus.routes.utils.getCurrentGuestId
import tech.nimbus.routes.utils.getPaginationParams
import tech.nimbus.routes.utils.getSortOrder
import tech.nimbus.routes.utils.respondError
import tech.nimbus.routes.utils.safeExecute
import tech.nimbus.services.PasteService
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import tech.nimbus.shared.dto.DeleteResponseDto
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.shared.dto.request.UpdatePasteRequestDto
import tech.nimbus.utils.DtoConverters.toPasteDtoWithAuthor
import tech.nimbus.utils.DtoConverters.toInternalVisibility
import tech.nimbus.utils.EtagUtil
import tech.nimbus.database.repositories.FavoritesRepository

/**
 * Роуты для работы с заметками.
 * Рефакторинг: вынесена бизнес-логика в PasteService,
 * добавлены утилиты для обработки запросов и ошибок.
 */
fun Route.pasteRoutes() {
    val pasteService = PasteService()
    val pasteRepository = PasteRepository()
    val favoritesRepository = FavoritesRepository()

    route("/api/pastes") {

        // GET /api/pastes/public - получить публичные заметки (optional auth для флага избранного)
        authenticate("auth-jwt", optional = true) {
            get("/public") {
                call.safeExecute {
                    val pagination = call.getPaginationParams()
                    val sortOrder = call.getSortOrder()
                    val currentUserId = call.getCurrentUserId()

                    val pasteResponses = pasteService.getPublicPastes(
                        limit = pagination.limit,
                        offset = pagination.offset,
                        sortOrder = sortOrder,
                        currentUserId = currentUserId
                    )

                    call.respond(HttpStatusCode.OK, pasteResponses)
                }
            }
        }

        // GET /api/pastes/{id} - получить заметку по ID
        authenticate("auth-jwt", optional = true) {
            get("/{id}") {
                call.safeExecute {
                    val id = call.parameters["id"]
                        ?: return@safeExecute call.respondError(
                            HttpStatusCode.BadRequest,
                            "Missing paste ID"
                        )

                    val userId = call.getCurrentUserId()
                    val guestId = call.getCurrentGuestId()
                    val pasteDto = pasteService.getPasteById(id, userId, guestId)

                    if (pasteDto != null) {
                        pasteDto.etag?.let { etag ->
                            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
                        }
                        call.respond(HttpStatusCode.OK, pasteDto)
                    } else {
                        call.respondError(
                            HttpStatusCode.NotFound,
                            "Access denied or paste not found"
                        )
                    }
                }
            }
        }

        // POST /api/pastes - создать заметку
        authenticate("auth-jwt", optional = true) {
            post {
                call.safeExecute {
                    val request = call.receive<CreatePasteRequestDto>()
                    val userId = call.getCurrentUserId()
                    val guestId = call.getCurrentGuestId()

                    val createdPaste = pasteService.createPaste(request, userId, guestId)
                    call.respond(HttpStatusCode.Created, createdPaste)
                }
            }
        }

        // GET /api/pastes/my - получить мои заметки
        authenticate("auth-jwt") {
            get("/my") {
                call.safeExecute {
                    val userId = call.getCurrentUserId()
                    val guestId = call.getCurrentGuestId()

                    val pagination = call.getPaginationParams()
                    val sortOrder = call.getSortOrder()
                    val favoriteOnly = call.request.queryParameters["favorite"]?.let { it.equals("true", true) } ?: false

                    when {
                        userId != null -> {
                            val userPastes = pasteService.getUserPastes(
                                userId = userId,
                                limit = pagination.limit,
                                offset = pagination.offset,
                                sortOrder = sortOrder,
                                favoriteOnly = favoriteOnly
                            )
                            call.respond(HttpStatusCode.OK, userPastes)
                        }
                        guestId != null -> {
                            if (favoriteOnly) {
                                // Избранное для гостя пока не поддержано на уровне БД
                                return@safeExecute call.respondError(HttpStatusCode.NotImplemented, "Guest favorites not supported yet")
                            }
                            val guestPastes = pasteService.getGuestPastes(
                                guestId = guestId,
                                limit = pagination.limit,
                                offset = pagination.offset,
                                sortOrder = sortOrder
                            )
                            call.respond(HttpStatusCode.OK, guestPastes)
                        }
                        else -> {
                            return@safeExecute call.respondError(
                                HttpStatusCode.Unauthorized,
                                "Authentication required"
                            )
                        }
                    }
                }
            }

            // POST /api/pastes/{id}/favorite - добавить в избранное
            post("/{id}/favorite") {
                call.safeExecute {
                    val id = call.parameters["id"]
                        ?: return@safeExecute call.respondError(HttpStatusCode.BadRequest, "Missing paste ID")

                    val userId = call.getCurrentUserId()
                        ?: return@safeExecute call.respondError(HttpStatusCode.Unauthorized, "Authentication required")

                    // Проверяем доступность заметки для пользователя
                    if (!pasteRepository.canAccessPaste(id, userId)) {
                        return@safeExecute call.respondError(HttpStatusCode.NotFound, "Paste not found")
                    }

                    favoritesRepository.addFavorite(userId, id)
                    call.respond(HttpStatusCode.OK, DeleteResponseDto("Added to favorites"))
                }
            }

            // DELETE /api/pastes/{id}/favorite - удалить из избранного
            delete("/{id}/favorite") {
                call.safeExecute {
                    val id = call.parameters["id"]
                        ?: return@safeExecute call.respondError(HttpStatusCode.BadRequest, "Missing paste ID")

                    val userId = call.getCurrentUserId()
                        ?: return@safeExecute call.respondError(HttpStatusCode.Unauthorized, "Authentication required")

                    favoritesRepository.removeFavorite(userId, id)
                    call.respond(HttpStatusCode.OK, DeleteResponseDto("Removed from favorites"))
                }
            }

            // DELETE /api/pastes/{id} - удалить заметку
            delete("/{id}") {
                call.safeExecute {
                    val id = call.parameters["id"]
                        ?: return@safeExecute call.respondError(
                            HttpStatusCode.BadRequest,
                            "Missing paste ID"
                        )

                    val userId = call.getCurrentUserId()
                    val guestId = call.getCurrentGuestId()

                    val deleted = when {
                        userId != null -> pasteService.deletePaste(id, userId)
                        guestId != null -> pasteRepository.deletePasteByGuest(id, guestId)
                        else -> false
                    }

                    if (deleted) {
                        call.respond(
                            HttpStatusCode.OK,
                            DeleteResponseDto("Paste deleted successfully")
                        )
                    } else {
                        call.respondError(
                            HttpStatusCode.NotFound,
                            "Paste not found or not owned"
                        )
                    }
                }
            }

            // PUT /api/pastes/{id} - обновить заметку с поддержкой ETag (If-Match)
            put("/{id}") {
                call.safeExecute {
                    val id = call.parameters["id"]
                        ?: return@safeExecute call.respondError(HttpStatusCode.BadRequest, "Missing paste ID")

                    val userId = call.getCurrentUserId()
                    val guestId = call.getCurrentGuestId()

                    val existing = pasteRepository.getPasteById(id)
                        ?: return@safeExecute call.respondError(HttpStatusCode.NotFound, "Paste not found")

                    val ownerOk = when {
                        userId != null -> existing.userId == userId
                        guestId != null -> existing.userId == null && existing.guestId == guestId
                        else -> false
                    }
                    if (!ownerOk) {
                        return@safeExecute call.respondError(HttpStatusCode.Forbidden, "Only the owner can update the paste")
                    }

                    val ifMatch = call.request.headers[HttpHeaders.IfMatch]
                        ?.trim()?.removePrefix("\"")?.removeSuffix("\"")
                        ?: return@safeExecute call.respondError(HttpStatusCode(428, "Precondition Required"), "If-Match header required")

                    val req = call.receive<UpdatePasteRequestDto>()

                    val updated = pasteRepository.updatePaste(
                        pasteId = id,
                        title = req.title,
                        content = req.content,
                        syntaxLanguage = req.syntaxLanguage,
                        visibility = req.visibility?.toInternalVisibility(),
                        expiresAt = req.expiresAt,
                        expectedEtag = ifMatch
                    ) ?: return@safeExecute call.respondError(HttpStatusCode.PreconditionFailed, "ETag mismatch or update rejected")

                    val withAuthor = pasteRepository.getPasteWithAuthor(id)
                    val dto = withAuthor?.paste?.toPasteDtoWithAuthor(withAuthor.author)
                        ?: updated.let { it.toPasteDtoWithAuthor(null) }

                    dto.etag?.let { etag -> call.response.headers.append(HttpHeaders.ETag, "\"$etag\"") }
                    call.respond(HttpStatusCode.OK, dto)
                }
            }
        }
    }
}
