package tech.nimbus.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.nimbus.routes.utils.getCurrentUserId
import tech.nimbus.routes.utils.getPaginationParams
import tech.nimbus.routes.utils.getSortOrder
import tech.nimbus.routes.utils.respondError
import tech.nimbus.routes.utils.safeExecute
import tech.nimbus.services.PasteService
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import tech.nimbus.shared.dto.DeleteResponseDto
import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.models.PasteVisibility
import tech.nimbus.models.request.UpdatePasteRequest
import tech.nimbus.utils.EtagUtil

/**
 * Роуты для работы с заметками.
 * Рефакторинг: вынесена бизнес-логика в PasteService,
 * добавлены утилиты для обработки запросов и ошибок.
 */
fun Route.pasteRoutes() {
    val pasteService = PasteService()
    val pasteRepository = PasteRepository()

    route("/api/pastes") {

        // GET /api/pastes/public - получить публичные заметки
        get("/public") {
            call.safeExecute {
                val pagination = call.getPaginationParams()
                val sortOrder = call.getSortOrder()

                val pasteResponses = pasteService.getPublicPastes(
                    limit = pagination.limit,
                    offset = pagination.offset,
                    sortOrder = sortOrder
                )

                call.respond(HttpStatusCode.OK, pasteResponses)
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
                    val pasteDto = pasteService.getPasteById(id, userId)

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

                    val createdPaste = pasteService.createPaste(request, userId)
                    call.respond(HttpStatusCode.Created, createdPaste)
                }
            }
        }

        // GET /api/pastes/my - получить мои заметки
        authenticate("auth-jwt") {
            get("/my") {
                call.safeExecute {
                    val userId = call.getCurrentUserId()
                        ?: return@safeExecute call.respondError(
                            HttpStatusCode.Unauthorized,
                            "Authentication required"
                        )

                    val pagination = call.getPaginationParams()
                    val sortOrder = call.getSortOrder()

                    val userPastes = pasteService.getUserPastes(
                        userId = userId,
                        limit = pagination.limit,
                        offset = pagination.offset,
                        sortOrder = sortOrder
                    )

                    call.respond(HttpStatusCode.OK, userPastes)
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
                        ?: return@safeExecute call.respondError(
                            HttpStatusCode.Unauthorized,
                            "Authentication required"
                        )

                    val deleted = pasteService.deletePaste(id, userId)

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
                        ?: return@safeExecute call.respondError(HttpStatusCode.Unauthorized, "Authentication required")

                    // Проверяем, что заметка существует и принадлежит пользователю
                    val existing = pasteRepository.getPasteById(id)
                        ?: return@safeExecute call.respondError(HttpStatusCode.NotFound, "Paste not found")

                    if (existing.userId == null || existing.userId != userId) {
                        return@safeExecute call.respondError(HttpStatusCode.Forbidden, "Only the owner can update the paste")
                    }

                    val ifMatch = call.request.headers[HttpHeaders.IfMatch]
                        ?.trim()?.removePrefix("\"")?.removeSuffix("\"")
                        ?: return@safeExecute call.respondError(HttpStatusCode(428, "Precondition Required"), "If-Match header required")

                    val req = call.receive<UpdatePasteRequest>()

                    val newVisibility = req.visibility?.let { vis ->
                        try { PasteVisibility.valueOf(vis.uppercase()) } catch (_: Exception) { null }
                    }

                    val updated = pasteRepository.updatePaste(
                        pasteId = id,
                        title = req.title,
                        content = req.content,
                        syntaxLanguage = req.syntaxLanguage,
                        visibility = newVisibility,
                        expiresAt = req.expiresAt,
                        expectedEtag = ifMatch
                    ) ?: return@safeExecute call.respondError(HttpStatusCode.PreconditionFailed, "ETag mismatch or update rejected")

                    val etag = EtagUtil.compute(updated.content, updated.updatedAt)
                    call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
                    call.respond(HttpStatusCode.OK, updated)
                }
            }
        }
    }
}
