package tech.nimbus.database.repositories

import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.database.repositories.interfaces.IPasteRepository
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import tech.nimbus.utils.EtagUtil
import tech.nimbus.utils.DtoConverters.normalizeSyntaxLanguage

/**
 * Енум для типов сортировки заметок.
 */
enum class PasteSortOrder {
    NEWEST_FIRST,   // По дате создания (новые сначала) - по умолчанию
    OLDEST_FIRST,   // По дате создания (старые сначала)
    MOST_VIEWED,    // По количеству просмотров (больше сначала)
    LEAST_VIEWED,   // По количеству просмотров (меньше сначала)
    TITLE_ASC,      // По названию (А-Я)
    TITLE_DESC      // По названию (Я-А)
}

/**
 * Результат запроса заметок с информацией об авторе.
 */
data class PasteWithAuthor(
    val paste: Paste,
    val author: User?
)

/**
 * Репозиторий для работы с заметками.
 *
 * Обеспечивает слой доступа к данным для всех операций с текстовыми заметками.
 * Поддерживает создание, чтение, обновление и удаление заметок с учетом
 * трех типов видимости (PUBLIC, UNLISTED, PRIVATE) и пагинации результатов.
 */
class PasteRepository : IPasteRepository {

    /**
     * Создает новую заметку в базе данных.
     *
     * Сохраняет заметку с уникальным идентификатором и возвращает созданный объект.
     * Автоматически устанавливает счетчик просмотров в 0.
     *
     * @param paste Объект заметки для создания
     * @return Созданная заметка или null в случае ошибки
     */
    override suspend fun createPaste(paste: Paste): Paste? = transaction {
        val insertedId = PasteTable.insert {
            it[id]         = paste.id
            it[title]      = paste.title
            it[content]    = paste.content
            it[userId]     = paste.userId
            it[PasteTable.guestId] = paste.guestId
            it[visibility] = paste.visibility
            it[createdAt]  = LocalDateTime.parse(paste.createdAt)
            it[updatedAt]  = LocalDateTime.parse(paste.updatedAt)
            it[expiresAt]  = paste.expiresAt?.let(LocalDateTime::parse)
            it[syntaxLanguage]   = paste.syntaxLanguage
            it[viewCount]  = 0
        } get PasteTable.id

        // Возвращаем созданный объект
        paste.copy(id = insertedId)
    }

    /**
     * Получает заметку по уникальному идентификатору.
     *
     * Находит и возвращает заметку без изменения счетчика просмотров.
     * Для инкремента просмотров используйте incrementViewCount().
     *
     * @param pasteId Уникальный идентификатор заметки
     * @return Объект заметки или null если не найдена
     */
    override suspend fun getPasteById(pasteId: String): Paste? = transaction {
        PasteTable.selectAll()
            .where { PasteTable.id eq pasteId }
            .singleOrNull()
            ?.let { row ->
                mapRowToPaste(row)
            }
    }

    /**
     * Получает заметку по ID с информацией об авторе.
     *
     * @param pasteId Идентификатор заметки
     * @return PasteWithAuthor или null если заметка не найдена
     */
    override suspend fun getPasteWithAuthor(pasteId: String): PasteWithAuthor? = transaction {
        PasteTable
            .leftJoin(UserTable, { PasteTable.userId }, { UserTable.id })
            .selectAll()
            .where { PasteTable.id eq pasteId }
            .singleOrNull()
            ?.let { row ->
                val paste = mapRowToPaste(row)
                val author = if (row[PasteTable.userId] != null) {
                    try {
                        User.fromResultRow(row)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                PasteWithAuthor(paste, author)
            }
    }

    /**
     * Увеличивает счетчик просмотров заметки на 1.
     *
     * Используется при каждом просмотре заметки для ведения статистики.
     * Операция выполняется атомарно.
     *
     * @param pasteId Идентификатор заметки для инкремента
     */
    override suspend fun incrementViewCount(pasteId: String): Unit = transaction {
        val currentCount = PasteTable.selectAll()
            .where { PasteTable.id eq pasteId }
            .singleOrNull()
            ?.get(PasteTable.viewCount) ?: 0

        PasteTable.update({ PasteTable.id eq pasteId }) {
            it[viewCount] = currentCount + 1
        }
    }



    /**
     * Получает публичные заметки с сортировкой и пагинацией (расширенная версия).
     */
    override suspend fun getPublicPastes(
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<Paste> = transaction {
        val query = PasteTable.selectAll()
            .where { PasteTable.visibility eq PasteVisibility.PUBLIC }

        val sortedQuery = when (sortOrder) {
            PasteSortOrder.NEWEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.DESC)
            PasteSortOrder.OLDEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.ASC)
            PasteSortOrder.MOST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.DESC)
            PasteSortOrder.LEAST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.ASC)
            PasteSortOrder.TITLE_ASC -> query.orderBy(PasteTable.title, SortOrder.ASC)
            PasteSortOrder.TITLE_DESC -> query.orderBy(PasteTable.title, SortOrder.DESC)
        }

        sortedQuery
            .limit(limit, offset.toLong())
            .map { row -> mapRowToPaste(row) }
    }

    /**
     * Получает публичные заметки с информацией об авторах.
     */
    override suspend fun getPublicPastesWithAuthors(
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<PasteWithAuthor> = transaction {
        val query = PasteTable
            .leftJoin(UserTable, { PasteTable.userId }, { UserTable.id })
            .selectAll()
            .where { PasteTable.visibility eq PasteVisibility.PUBLIC }

        val sortedQuery = when (sortOrder) {
            PasteSortOrder.NEWEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.DESC)
            PasteSortOrder.OLDEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.ASC)
            PasteSortOrder.MOST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.DESC)
            PasteSortOrder.LEAST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.ASC)
            PasteSortOrder.TITLE_ASC -> query.orderBy(PasteTable.title, SortOrder.ASC)
            PasteSortOrder.TITLE_DESC -> query.orderBy(PasteTable.title, SortOrder.DESC)
        }

        sortedQuery
            .limit(limit, offset.toLong())
            .map { row ->
                val paste = mapRowToPaste(row)
                val author = if (row[PasteTable.userId] != null) {
                    try {
                        User.fromResultRow(row)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                PasteWithAuthor(paste, author)
            }
    }

    /**
     * Получает заметки пользователя с фильтрацией и сортировкой.
     */
    override suspend fun getUserPastes(
        userId: String,
        visibility: PasteVisibility?,
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<Paste> = transaction {
        var query = PasteTable.selectAll()
            .where { PasteTable.userId eq userId }

        visibility?.let { vis ->
            query = query.andWhere { PasteTable.visibility eq vis }
        }

        val sortedQuery = when (sortOrder) {
            PasteSortOrder.NEWEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.DESC)
            PasteSortOrder.OLDEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.ASC)
            PasteSortOrder.MOST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.DESC)
            PasteSortOrder.LEAST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.ASC)
            PasteSortOrder.TITLE_ASC -> query.orderBy(PasteTable.title, SortOrder.ASC)
            PasteSortOrder.TITLE_DESC -> query.orderBy(PasteTable.title, SortOrder.DESC)
        }

        sortedQuery
            .limit(limit, offset.toLong())
            .map { row -> mapRowToPaste(row) }
    }

    /**
     * Получает публичные заметки пользователя с информацией об авторе.
     */
    override suspend fun getUserPublicPastes(
        userId: String,
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<PasteWithAuthor> = transaction {
        val query = PasteTable
            .leftJoin(UserTable, { PasteTable.userId }, { UserTable.id })
            .selectAll()
            .where {
                (PasteTable.userId eq userId) and
                (PasteTable.visibility eq PasteVisibility.PUBLIC)
            }

        val sortedQuery = when (sortOrder) {
            PasteSortOrder.NEWEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.DESC)
            PasteSortOrder.OLDEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.ASC)
            PasteSortOrder.MOST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.DESC)
            PasteSortOrder.LEAST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.ASC)
            PasteSortOrder.TITLE_ASC -> query.orderBy(PasteTable.title, SortOrder.ASC)
            PasteSortOrder.TITLE_DESC -> query.orderBy(PasteTable.title, SortOrder.DESC)
        }

        sortedQuery
            .limit(limit, offset.toLong())
            .map { row ->
                val paste = mapRowToPaste(row)
                val author = User.fromResultRow(row)
                PasteWithAuthor(paste, author)
            }
    }

    /**
     * Проверяет доступ к заметке для указанного пользователя.
     */
    override suspend fun canAccessPaste(pasteId: String, userId: String?): Boolean = transaction {
        val paste = PasteTable.selectAll()
            .where { PasteTable.id eq pasteId }
            .singleOrNull()
            ?: return@transaction false

        when (paste[PasteTable.visibility]) {
            PasteVisibility.PUBLIC, PasteVisibility.UNLISTED -> true
            PasteVisibility.PRIVATE -> paste[PasteTable.userId] == userId
        }
    }

    /**
     * Получает заметки гостя с фильтрацией и сортировкой.
     */
    override suspend fun getGuestPastes(
        guestId: String,
        visibility: PasteVisibility?,
        limit: Int,
        offset: Int,
        sortOrder: PasteSortOrder
    ): List<Paste> = transaction {
        var query = PasteTable.selectAll()
            .where { PasteTable.guestId eq guestId }

        visibility?.let { vis ->
            query = query.andWhere { PasteTable.visibility eq vis }
        }

        val sortedQuery = when (sortOrder) {
            PasteSortOrder.NEWEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.DESC)
            PasteSortOrder.OLDEST_FIRST -> query.orderBy(PasteTable.createdAt, SortOrder.ASC)
            PasteSortOrder.MOST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.DESC)
            PasteSortOrder.LEAST_VIEWED -> query.orderBy(PasteTable.viewCount, SortOrder.ASC)
            PasteSortOrder.TITLE_ASC -> query.orderBy(PasteTable.title, SortOrder.ASC)
            PasteSortOrder.TITLE_DESC -> query.orderBy(PasteTable.title, SortOrder.DESC)
        }

        sortedQuery
            .limit(limit, offset.toLong())
            .map { row -> mapRowToPaste(row) }
    }

    /**
     * Подсчитывает общее количество публичных заметок.
     */
    override suspend fun getPublicPastesCount(): Long = transaction {
        PasteTable.selectAll()
            .where { PasteTable.visibility eq PasteVisibility.PUBLIC }
            .count()
    }

    /**
     * Подсчитывает общее количество заметок пользователя.
     */
    override suspend fun getUserPastesCount(userId: String): Long = transaction {
        PasteTable.selectAll()
            .where { PasteTable.userId eq userId }
            .count()
    }

    /**
     * Получает количество заметок пользователя по типу видимости.
     */
    override suspend fun getUserPastesCount(userId: String, visibility: PasteVisibility?): Long = transaction {
        var query = PasteTable.selectAll()
            .where { PasteTable.userId eq userId }

        visibility?.let { vis ->
            query = query.andWhere { PasteTable.visibility eq vis }
        }

        query.count()
    }

    /**
     * Удаляет заметку пользователя.
     */
    override suspend fun deletePaste(pasteId: String, userId: String): Boolean = transaction {
        PasteTable.deleteWhere {
            (PasteTable.id eq pasteId) and (PasteTable.userId eq userId)
        } > 0
    }

    /**
     * Удаляет заметку по guestId.
     */
    override suspend fun deletePasteByGuest(pasteId: String, guestId: String): Boolean = transaction {
        PasteTable.deleteWhere {
            (PasteTable.id eq pasteId) and (PasteTable.guestId eq guestId)
        } > 0
    }

    /**
     * Обновляет заметку с учетом ETag.
     */
    override suspend fun updatePaste(
        pasteId: String,
        title: String?,
        content: String?,
        syntaxLanguage: String?,
        visibility: PasteVisibility?,
        expiresAt: String?,
        expectedEtag: String?
    ): Paste? = transaction {
        val existing = PasteTable.selectAll().where { PasteTable.id eq pasteId }.singleOrNull()
            ?: return@transaction null

        val current = mapRowToPaste(existing)
        if (expectedEtag != null) {
            val currentEtag = EtagUtil.compute(current.content, current.updatedAt)
            if (!currentEtag.equals(expectedEtag, ignoreCase = true)) {
                return@transaction null
            }
        }

        val now = LocalDateTime.now()
        PasteTable.update({ PasteTable.id eq pasteId }) {
            title?.let { t -> it[PasteTable.title] = t }
            content?.let { c -> it[PasteTable.content] = c }
            normalizeSyntaxLanguage(syntaxLanguage)?.let { sl -> it[PasteTable.syntaxLanguage] = sl }
            visibility?.let { v -> it[PasteTable.visibility] = v }
            if (expiresAt != null) {
                it[PasteTable.expiresAt] = expiresAt.let(LocalDateTime::parse)
            }
            it[PasteTable.updatedAt] = now
        }

        PasteTable.selectAll().where { PasteTable.id eq pasteId }.singleOrNull()
            ?.let { row -> mapRowToPaste(row) }
    }

    /**
     * Вспомогательный метод для маппинга ResultRow в объект Paste.
     */
    private fun mapRowToPaste(row: ResultRow): Paste {
        return Paste(
            id = row[PasteTable.id],
            title = row[PasteTable.title],
            content = row[PasteTable.content],
            userId = row[PasteTable.userId],
            visibility = row[PasteTable.visibility],
            createdAt = row[PasteTable.createdAt].toString(),
            updatedAt = row[PasteTable.updatedAt].toString(),
            expiresAt = row[PasteTable.expiresAt]?.toString(),
            syntaxLanguage = row[PasteTable.syntaxLanguage],
            viewCount = row[PasteTable.viewCount],
            guestId = row[PasteTable.guestId]
        )
    }
}