package tech.nimbus.database.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tech.nimbus.database.repositories.interfaces.IFavoritesRepository
import tech.nimbus.database.tables.UserFavoritesTable

class FavoritesRepository : IFavoritesRepository {

    override suspend fun addFavorite(userId: String, pasteId: String): Boolean = transaction {
        try {
            UserFavoritesTable.insert {
                it[UserFavoritesTable.userId] = userId
                it[UserFavoritesTable.pasteId] = pasteId
            }
            true
        } catch (_: Exception) {
            // Конфликт PK (повтор) или иная ошибка — считаем как не добавлено
            false
        }
    }

    override suspend fun removeFavorite(userId: String, pasteId: String): Boolean = transaction {
        UserFavoritesTable.deleteWhere {
            (UserFavoritesTable.userId eq userId) and (UserFavoritesTable.pasteId eq pasteId)
        } > 0
    }

    override suspend fun isFavorite(userId: String, pasteId: String): Boolean = transaction {
        UserFavoritesTable.select {
            (UserFavoritesTable.userId eq userId) and (UserFavoritesTable.pasteId eq pasteId)
        }.limit(1).empty().not()
    }

    override suspend fun listFavoritePasteIds(userId: String, limit: Int, offset: Int): List<String> = transaction {
        UserFavoritesTable
            .select { UserFavoritesTable.userId eq userId }
            .limit(limit, offset.toLong())
            .map { it[UserFavoritesTable.pasteId] }
    }
}
