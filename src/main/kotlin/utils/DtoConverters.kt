package tech.nimbus.utils

import tech.nimbus.models.Paste
import tech.nimbus.models.User
import tech.nimbus.shared.dto.PasteDto
import tech.nimbus.shared.dto.UserDto
import tech.nimbus.shared.dto.PasteVisibility as SharedPasteVisibility
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import tech.nimbus.models.PasteVisibility as InternalPasteVisibility
import tech.nimbus.models.request.CreatePasteRequest

/**
 * Утилиты для конвертации между внутренними моделями и shared DTO.
 *
 * Обеспечивает совместимость между backend моделями и моделями shared модуля,
 * используемыми в Android приложении.
 */
object DtoConverters {

    /**
     * Конвертирует внутреннюю модель Paste в PasteDto для shared модуля.
     */
    fun Paste.toPasteDto(): PasteDto {
        return PasteDto(
            id = this.id,
            title = this.title,
            content = this.content,
            userId = this.userId,
            authorUsername = null,  // Будет заполнено в репозитории при JOIN
            authorDisplayName = null,  // Будет заполнено в репозитории при JOIN
            visibility = this.visibility.toSharedVisibility(),
            createdAt = this.createdAt,
            expiresAt = this.expiresAt,
            syntaxLanguage = this.syntaxLanguage,
            viewCount = this.viewCount
        )
    }

    /**
     * Конвертирует внутреннюю модель Paste с автором в PasteDto.
     */
    fun Paste.toPasteDtoWithAuthor(author: User?): PasteDto {
        return PasteDto(
            id = this.id,
            title = this.title,
            content = this.content,
            userId = this.userId,
            authorUsername = author?.username,
            authorDisplayName = author?.displayName,
            visibility = this.visibility.toSharedVisibility(),
            createdAt = this.createdAt,
            expiresAt = this.expiresAt,
            syntaxLanguage = this.syntaxLanguage,
            viewCount = this.viewCount
        )
    }

    /**
     * Конвертирует внутреннюю модель User в UserDto для shared модуля.
     */
    fun User.toUserDto(): UserDto {
        return UserDto(
            id = this.id,
            username = this.username,
            displayName = this.displayName,
            email = this.email,
            createdAt = this.createdAt
        )
    }

    /**
     * Конвертирует CreatePasteRequestDto из shared модуля во внутреннюю модель.
     */
//    fun CreatePasteRequestDto.toCreatePasteRequest(): CreatePasteRequest {
//        return CreatePasteRequest(
//            title = this.title,
//            content = this.content,
//            visibility = this.visibility.toInternalVisibility(),
//            expiresAt = this.expiresAt,
//            language = this.language
//        )
//    }

    /**
     * Конвертирует внутренний enum PasteVisibility в shared enum.
     */
    fun InternalPasteVisibility.toSharedVisibility(): SharedPasteVisibility {
        return when (this) {
            InternalPasteVisibility.PUBLIC -> SharedPasteVisibility.PUBLIC
            InternalPasteVisibility.UNLISTED -> SharedPasteVisibility.UNLISTED
            InternalPasteVisibility.PRIVATE -> SharedPasteVisibility.PRIVATE
        }
    }

    /**
     * Конвертирует shared enum PasteVisibility во внутренний enum.
     */
    fun SharedPasteVisibility.toInternalVisibility(): InternalPasteVisibility {
        return when (this) {
            SharedPasteVisibility.PUBLIC -> InternalPasteVisibility.PUBLIC
            SharedPasteVisibility.UNLISTED -> InternalPasteVisibility.UNLISTED
            SharedPasteVisibility.PRIVATE -> InternalPasteVisibility.PRIVATE
        }
    }
}
