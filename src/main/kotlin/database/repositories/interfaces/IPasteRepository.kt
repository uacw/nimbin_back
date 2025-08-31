package tech.nimbus.database.repositories.interfaces

import tech.nimbus.database.repositories.PasteWithAuthor
import tech.nimbus.database.repositories.PasteSortOrder
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility

/**
 * Интерфейс репозитория для работы с заметками.
 * Определяет контракт для всех операций с заметками.
 */
interface IPasteRepository {

    /**
     * Создает новую заметку.
     */
    suspend fun createPaste(paste: Paste): Paste?

    /**
     * Получает заметку по ID.
     */
    suspend fun getPasteById(pasteId: String): Paste?

    /**
     * Получает заметку с информацией об авторе по ID.
     */
    suspend fun getPasteWithAuthor(pasteId: String): PasteWithAuthor?

    /**
     * Увеличивает счетчик просмотров заметки.
     */
    suspend fun incrementViewCount(pasteId: String)

    /**
     * Получает публичные заметки с сортировкой и пагинацией.
     */
    suspend fun getPublicPastes(
        limit: Int = 20,
        offset: Int = 0,
        sortOrder: PasteSortOrder = PasteSortOrder.NEWEST_FIRST
    ): List<Paste>

    /**
     * Получает публичные заметки с информацией об авторах.
     */
    suspend fun getPublicPastesWithAuthors(
        limit: Int = 20,
        offset: Int = 0,
        sortOrder: PasteSortOrder = PasteSortOrder.NEWEST_FIRST
    ): List<PasteWithAuthor>

    /**
     * Получает заметки пользователя.
     */
    suspend fun getUserPastes(
        userId: String,
        visibility: PasteVisibility? = null,
        limit: Int = 20,
        offset: Int = 0,
        sortOrder: PasteSortOrder = PasteSortOrder.NEWEST_FIRST
    ): List<Paste>

    /**
     * Получает публичные заметки пользователя с информацией об авторе.
     */
    suspend fun getUserPublicPastes(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
        sortOrder: PasteSortOrder = PasteSortOrder.NEWEST_FIRST
    ): List<PasteWithAuthor>

    /**
     * Проверяет доступ к заметке.
     */
    suspend fun canAccessPaste(pasteId: String, userId: String?): Boolean

    /**
     * Получает количество публичных заметок.
     */
    suspend fun getPublicPastesCount(): Long

    /**
     * Получает количество заметок пользователя.
     */
    suspend fun getUserPastesCount(userId: String): Long

    /**
     * Получает количество заметок пользователя по типу видимости.
     */
    suspend fun getUserPastesCount(userId: String, visibility: PasteVisibility? = null): Long

    /**
     * Удаляет заметку пользователя.
     */
    suspend fun deletePaste(pasteId: String, userId: String): Boolean
}
