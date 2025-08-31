package tech.nimbus.shared.api

/**
 * Константы API endpoints для Nimbin backend.
 */
object ApiEndpoints {
    
    // Base paths
    const val API_BASE = "/api"
    
    // Paste endpoints
    const val PASTES = "$API_BASE/pastes"
    const val PASTE_BY_ID = "$PASTES/{id}"
    const val PUBLIC_PASTES = "$PASTES/public"
    const val USER_PASTES = "$PASTES/my"
    
    // Auth endpoints
    const val AUTH_BASE = "$API_BASE/auth"
    const val REGISTER = "$AUTH_BASE/register"
    const val LOGIN = "$AUTH_BASE/login"
    
    // User endpoints
    const val USER_BASE = "$API_BASE/users"
    const val USER_PROFILE = "$USER_BASE/profile"
    const val USER_BY_ID = "$USER_BASE/{id}"
    const val USER_PUBLIC_PASTES = "$USER_BASE/{id}/pastes"
    
    /**
     * Создать URL для получения заметки по ID
     */
    fun pasteById(id: String): String = PASTES + "/$id"
    
    /**
     * Создать URL для публичных заметок с параметрами пагинации
     */
    fun publicPastes(page: Int = 1, limit: Int = 20): String = 
        "$PUBLIC_PASTES?page=$page&limit=$limit"
    
    /**
     * Создать URL для заметок пользователя с параметрами пагинации
     */
    fun userPastes(page: Int = 1, limit: Int = 20): String = 
        "$USER_PASTES?page=$page&limit=$limit"
    
    /**
     * Создать URL для профиля пользователя по ID
     */
    fun userProfileById(userId: String): String = USER_BASE + "/$userId"
    
    /**
     * Создать URL для публичных заметок пользователя по ID
     */
    fun userPublicPastes(userId: String, page: Int = 1, limit: Int = 20): String = 
        "$USER_BASE/$userId/pastes?page=$page&limit=$limit"
}

/**
 * HTTP заголовки для работы с API
 */
object ApiHeaders {
    const val AUTHORIZATION = "Authorization"
    const val CONTENT_TYPE = "Content-Type"
    const val ACCEPT = "Accept"
    
    const val APPLICATION_JSON = "application/json"
    
    /**
     * Создать Bearer токен для авторизации
     */
    fun bearerToken(token: String): String = "Bearer $token"
}
