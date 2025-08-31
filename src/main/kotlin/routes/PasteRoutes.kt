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

/**
 * Роуты для работы с заметками.
 * Рефакторинг: вынесена бизнес-логика в PasteService,
 * добавлены утилиты для обработки запросов и ошибок.
 */
fun Route.pasteRoutes() {
    val pasteService = PasteService()

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
        }
    }
}
