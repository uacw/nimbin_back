package tech.nimbus.services

import tech.nimbus.database.repositories.PasteRepository
import tech.nimbus.database.repositories.PasteSortOrder
import tech.nimbus.database.repositories.interfaces.IPasteRepository
import tech.nimbus.shared.dto.PasteDto
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import tech.nimbus.utils.DtoConverters.toPasteDto
import tech.nimbus.utils.DtoConverters.toPasteDtoWithAuthor
import tech.nimbus.utils.DtoConverters.toInternalVisibility
import tech.nimbus.utils.generatePasteId
import tech.nimbus.validation.ValidationService
import tech.nimbus.validation.ValidationResult
import tech.nimbus.exceptions.*
import tech.nimbus.models.Paste
import java.time.LocalDateTime
import tech.nimbus.utils.DtoConverters.normalizeSyntaxLanguage

/**
 * Сервис для работы с заметками.
 * Содержит бизнес-логику, валидацию и обработку ошибок.
 */
class PasteService(
    private val repository: IPasteRepository = PasteRepository()
) {

    /**
     * Получить публичные заметки с пагинацией и сортировкой.
     */
    suspend fun getPublicPastes(
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<PasteDto> {
        validatePaginationParams(limit, offset)

        try {
            val pastesWithAuthors = repository.getPublicPastesWithAuthors(limit, offset, sortOrder)
            return pastesWithAuthors.map { (paste, author) ->
                paste.toPasteDtoWithAuthor(author)
            }
        } catch (e: Exception) {
            throw DatabaseException.QueryFailed("getPublicPastesWithAuthors", e)
        }
    }

    /**
     * Получить заметку по ID с проверкой доступа.
     */
    suspend fun getPasteById(pasteId: String, userId: String?): PasteDto? {
        validatePasteId(pasteId)

        try {
            if (!repository.canAccessPaste(pasteId, userId)) {
                return null // Вместо исключения возвращаем null для совместимости с роутами
            }

            val pasteWithAuthor = repository.getPasteWithAuthor(pasteId)
                ?: return null

            // Увеличиваем счетчик просмотров
            repository.incrementViewCount(pasteId)

            return pasteWithAuthor.paste.toPasteDtoWithAuthor(pasteWithAuthor.author)
        } catch (e: Exception) {
            throw DatabaseException.QueryFailed("getPasteWithAuthor", e)
        }
    }

    /**
     * Создать новую заметку.
     */
    suspend fun createPaste(request: CreatePasteRequestDto, userId: String?): PasteDto {
        // Валидация входных данных
        val validationResult = ValidationService.validatePasteCreation(request.title, request.content)
        if (validationResult !is ValidationResult.Success) {
            throw ValidationException.InvalidInput(getValidationErrorMessage(validationResult))
        }

        // Проверяем требования для приватных заметок
        if (request.visibility == tech.nimbus.shared.dto.PasteVisibility.PRIVATE && userId == null) {
            throw PasteException.PrivatePasteRequiresAuth()
        }

        val nowIso = LocalDateTime.now().toString()
        val paste = Paste(
            id = generatePasteId(),
            title = request.title.trim(),
            content = request.content,
            userId = userId,
            visibility = request.visibility.toInternalVisibility(),
            createdAt = nowIso,
            updatedAt = nowIso,
            expiresAt = request.expiresAt,
            syntaxLanguage = normalizeSyntaxLanguage(request.syntaxLanguage) ?: "plaintext"
        )

        try {
            val created = repository.createPaste(paste)
                ?: throw PasteException.PasteCreationFailed()

            // Возвращаем созданную заметку с информацией об авторе
            val pasteWithAuthor = repository.getPasteWithAuthor(created.id)
            return if (pasteWithAuthor != null) {
                pasteWithAuthor.paste.toPasteDtoWithAuthor(pasteWithAuthor.author)
            } else {
                created.toPasteDto()
            }
        } catch (e: PasteException) {
            throw e
        } catch (e: Exception) {
            throw DatabaseException.QueryFailed("createPaste", e)
        }
    }

    /**
     * Получить заметки пользователя.
     */
    suspend fun getUserPastes(
        userId: String,
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder = PasteSortOrder.NEWEST_FIRST
    ): List<PasteDto> {
        validatePaginationParams(limit, offset)
        validateUserId(userId)

        try {
            val userPastes = repository.getUserPastes(userId, null, limit, offset, sortOrder)
            return userPastes.map { it.toPasteDto() }
        } catch (e: Exception) {
            throw DatabaseException.QueryFailed("getUserPastes", e)
        }
    }

    /**
     * Удалить заметку пользователя.
     */
    suspend fun deletePaste(pasteId: String, userId: String): Boolean {
        validatePasteId(pasteId)
        validateUserId(userId)

        try {
            val deleted = repository.deletePaste(pasteId, userId)
            if (!deleted) {
                return false // Вместо исключения возвращаем false для совместимости
            }
            return true
        } catch (e: Exception) {
            throw DatabaseException.QueryFailed("deletePaste", e)
        }
    }

    // Приватные методы валидации

    private fun validatePaginationParams(limit: Int, offset: Int) {
        if (limit !in 1..100) {
            throw PaginationException.InvalidLimit(limit)
        }
        if (offset < 0) {
            throw PaginationException.InvalidOffset(offset)
        }
    }

    private fun validatePasteId(pasteId: String) {
        when {
            pasteId.isBlank() -> throw ValidationException.RequiredField("pasteId")
            pasteId.length != 12 -> throw PasteException.InvalidPasteId(pasteId)
        }
    }

    private fun validateUserId(userId: String) {
        if (userId.isBlank()) {
            throw ValidationException.RequiredField("userId")
        }
    }

    private fun getValidationErrorMessage(result: ValidationResult): String {
        return when (result) {
            is ValidationResult.Error -> result.message
            is ValidationResult.Errors -> result.errors.joinToString("; ") { it.message }
            is ValidationResult.Success -> ""
        }
    }
}
