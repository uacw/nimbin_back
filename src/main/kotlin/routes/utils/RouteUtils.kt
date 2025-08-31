package tech.nimbus.routes.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import tech.nimbus.shared.dto.ApiErrorDto
import tech.nimbus.database.repositories.PasteSortOrder
import tech.nimbus.exceptions.*
import tech.nimbus.validation.ValidationResult

/**
 * Константы для пагинации.
 */
object Pagination {
    const val DEFAULT_PAGE = 1
    const val DEFAULT_LIMIT = 20
    const val MAX_LIMIT = 100
}

/**
 * Константы для сортировки.
 */
object SortParams {
    const val NEWEST = "newest"
    const val OLDEST = "oldest"
    const val MOST_VIEWED = "most_viewed"
    const val LEAST_VIEWED = "least_viewed"
    const val TITLE_ASC = "title_asc"
    const val TITLE_DESC = "title_desc"
}

/**
 * Параметры пагинации.
 */
data class PaginationParams(
    val page: Int,
    val limit: Int,
    val offset: Int
)

/**
 * Получить параметры пагинации из запроса с валидацией.
 */
fun ApplicationCall.getPaginationParams(): PaginationParams {
    val page = request.queryParameters["page"]?.toIntOrNull()
        ?.takeIf { it > 0 } ?: Pagination.DEFAULT_PAGE

    val limit = request.queryParameters["limit"]?.toIntOrNull()
        ?.takeIf { it in 1..Pagination.MAX_LIMIT } ?: Pagination.DEFAULT_LIMIT

    return PaginationParams(page, limit, (page - 1) * limit)
}

/**
 * Получить параметр сортировки из запроса.
 */
fun ApplicationCall.getSortOrder(): PasteSortOrder {
    val sortParam = request.queryParameters["sort"]?.lowercase() ?: SortParams.NEWEST
    return when (sortParam) {
        SortParams.NEWEST -> PasteSortOrder.NEWEST_FIRST
        SortParams.OLDEST -> PasteSortOrder.OLDEST_FIRST
        SortParams.MOST_VIEWED -> PasteSortOrder.MOST_VIEWED
        SortParams.LEAST_VIEWED -> PasteSortOrder.LEAST_VIEWED
        SortParams.TITLE_ASC -> PasteSortOrder.TITLE_ASC
        SortParams.TITLE_DESC -> PasteSortOrder.TITLE_DESC
        else -> PasteSortOrder.NEWEST_FIRST
    }
}

/**
 * Получить ID пользователя из JWT токена.
 */
fun ApplicationCall.getCurrentUserId(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
}

/**
 * Безопасный способ отправки ответа об ошибке.
 */
suspend fun ApplicationCall.respondError(
    statusCode: HttpStatusCode,
    message: String,
    cause: Throwable? = null
) {
    val errorMessage = if (cause != null && statusCode == HttpStatusCode.InternalServerError) {
        "Internal server error: ${cause.message}"
    } else {
        message
    }
    respond(statusCode, ApiErrorDto(errorMessage))
}

/**
 * Выполнить операцию с расширенной обработкой исключений.
 */
suspend inline fun ApplicationCall.safeExecute(
    operation: suspend () -> Unit
) {
    try {
        operation()
    } catch (e: AuthException) {
        handleAuthException(e)
    } catch (e: UserException) {
        handleUserException(e)
    } catch (e: PasteException) {
        handlePasteException(e)
    } catch (e: ValidationException) {
        handleValidationException(e)
    } catch (e: PaginationException) {
        respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid pagination parameters")
    } catch (e: DatabaseException) {
        respondError(HttpStatusCode.InternalServerError, "Database error occurred")
    } catch (e: IllegalArgumentException) {
        respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid request", e)
    } catch (e: Exception) {
        respondError(HttpStatusCode.InternalServerError, "Operation failed", e)
    }
}

/**
 * Обработка исключений аутентификации.
 */
suspend fun ApplicationCall.handleAuthException(e: AuthException) {
    when (e) {
        is AuthException.InvalidCredentials,
        is AuthException.UserNotFound,
        is AuthException.IncorrectPassword -> {
            respondError(HttpStatusCode.Unauthorized, "Invalid credentials")
        }
        is AuthException.AuthenticationRequired -> {
            respondError(HttpStatusCode.Unauthorized, "Authentication required")
        }
        is AuthException.TokenExpired,
        is AuthException.InvalidToken -> {
            respondError(HttpStatusCode.Unauthorized, "Invalid or expired token")
        }
    }
}

/**
 * Обработка исключений пользователей.
 */
suspend fun ApplicationCall.handleUserException(e: UserException) {
    when (e) {
        is UserException.UserAlreadyExists,
        is UserException.UsernameAlreadyExists,
        is UserException.EmailAlreadyExists -> {
            respondError(HttpStatusCode.Conflict, e.message ?: "User already exists")
        }
        is UserException.UserNotFound -> {
            respondError(HttpStatusCode.NotFound, "User not found")
        }
        is UserException.UserCreationFailed -> {
            respondError(HttpStatusCode.InternalServerError, "User creation failed")
        }
        is UserException.ProfileUpdateFailed -> {
            respondError(HttpStatusCode.InternalServerError, "Profile update failed")
        }
    }
}

/**
 * Обработка исключений заметок.
 */
suspend fun ApplicationCall.handlePasteException(e: PasteException) {
    when (e) {
        is PasteException.PasteNotFound -> {
            respondError(HttpStatusCode.NotFound, "Paste not found")
        }
        is PasteException.AccessDenied -> {
            respondError(HttpStatusCode.Forbidden, "Access denied")
        }
        is PasteException.PrivatePasteRequiresAuth -> {
            respondError(HttpStatusCode.Unauthorized, "Private paste requires authentication")
        }
        is PasteException.PasteCreationFailed,
        is PasteException.PasteDeletionFailed -> {
            respondError(HttpStatusCode.InternalServerError, e.message ?: "Paste operation failed")
        }
        is PasteException.InvalidPasteId -> {
            respondError(HttpStatusCode.BadRequest, "Invalid paste ID")
        }
    }
}

/**
 * Обработка исключений валидации.
 */
suspend fun ApplicationCall.handleValidationException(e: ValidationException) {
    when (e) {
        is ValidationException.InvalidInput,
        is ValidationException.RequiredField -> {
            respondError(HttpStatusCode.BadRequest, e.message ?: "Validation error")
        }
        is ValidationException.InvalidFormat,
        is ValidationException.ValueTooLong,
        is ValidationException.ValueTooShort -> {
            respondError(HttpStatusCode.BadRequest, e.message ?: "Validation error")
        }
    }
}

/**
 * Обработка результата валидации.
 */
suspend fun ApplicationCall.handleValidationResult(result: ValidationResult): Boolean {
    return when (result) {
        is ValidationResult.Success -> true
        is ValidationResult.Error -> {
            respondError(HttpStatusCode.BadRequest, result.message)
            false
        }
        is ValidationResult.Errors -> {
            val errorMessage = result.errors.joinToString("; ") { "${it.field ?: "field"}: ${it.message}" }
            respondError(HttpStatusCode.BadRequest, errorMessage)
            false
        }
    }
}
