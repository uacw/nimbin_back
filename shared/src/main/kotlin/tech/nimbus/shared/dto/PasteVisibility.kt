package tech.nimbus.shared.dto

import kotlinx.serialization.Serializable

/**
 * Типы видимости заметок для multiplatform использования.
 */
@Serializable
enum class PasteVisibility {
    /**
     * Публичная заметка - отображается в общем списке
     */
    PUBLIC,

    /**
     * Скрытая заметка - доступна только по прямой ссылке
     */
    UNLISTED,

    /**
     * Приватная заметка - доступна только владельцу
     */
    PRIVATE
}
