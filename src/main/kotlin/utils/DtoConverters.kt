package tech.nimbus.utils

import tech.nimbus.models.Paste
import tech.nimbus.models.User
import tech.nimbus.shared.dto.PasteDto
import tech.nimbus.shared.dto.UserDto
import tech.nimbus.shared.dto.PasteVisibility as SharedPasteVisibility
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import tech.nimbus.models.PasteVisibility as InternalPasteVisibility

/**
 * Утилиты для конвертации между внутренними моделями и shared DTO.
 *
 * Обеспечивает совместимость между backend моделями и моделями shared модуля,
 * используемыми в Android приложении.
 */
object DtoConverters {

    /** Нормализация значения языка подсветки. */
    fun normalizeSyntaxLanguage(value: String?): String? {
        val v = value?.trim()?.lowercase() ?: return null
        if (v.isBlank()) return "plaintext"
        return when (v) {
            "text" -> "plaintext"
            else -> v
        }
    }

    /**
     * Конвертирует внутреннюю модель Paste в PasteDto для shared модуля.
     */
    fun Paste.toPasteDto(): PasteDto {
        val etag = EtagUtil.compute(this.content, this.updatedAt)
        return PasteDto(
            id = this.id,
            title = this.title,
            content = this.content,
            userId = this.userId,
            authorUsername = null,  // Будет заполнено в репозитории при JOIN
            authorDisplayName = null,  // Будет заполнено в репозитории при JOIN
            visibility = this.visibility.toSharedVisibility(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            expiresAt = this.expiresAt,
            syntaxLanguage = this.syntaxLanguage,
            viewCount = this.viewCount,
            etag = etag
        )
    }

    /**
     * Конвертирует внутреннюю модель Paste с автором в PasteDto.
     */
    fun Paste.toPasteDtoWithAuthor(author: User?): PasteDto {
        val etag = EtagUtil.compute(this.content, this.updatedAt)
        return PasteDto(
            id = this.id,
            title = this.title,
            content = this.content,
            userId = this.userId,
            authorUsername = author?.username,
            authorDisplayName = author?.displayName,
            visibility = this.visibility.toSharedVisibility(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            expiresAt = this.expiresAt,
            syntaxLanguage = this.syntaxLanguage,
            viewCount = this.viewCount,
            etag = etag
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
