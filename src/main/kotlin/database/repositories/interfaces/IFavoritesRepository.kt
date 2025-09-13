package tech.nimbus.database.repositories.interfaces

/**
 * Контракт репозитория избранного.
 */
interface IFavoritesRepository {
    suspend fun addFavorite(userId: String, pasteId: String): Boolean
    suspend fun removeFavorite(userId: String, pasteId: String): Boolean
    suspend fun isFavorite(userId: String, pasteId: String): Boolean
    suspend fun listFavoritePasteIds(userId: String, limit: Int = 100, offset: Int = 0): List<String>
}

