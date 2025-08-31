package tech.nimbus.shared.api

import tech.nimbus.shared.dto.*
import tech.nimbus.shared.dto.request.*
import tech.nimbus.shared.utils.ApiResult

/**
 * Интерфейс API клиента для взаимодействия с Nimbin backend.
 * 
 * Этот интерфейс определяет все доступные методы API и может быть
 * реализован на каждой платформе (Android, iOS) с использованием
 * соответствующих HTTP клиентов.
 */
interface NimbinApiClient {
    
    // === Paste Operations ===
    
    /**
     * Создать новую заметку
     */
    suspend fun createPaste(request: CreatePasteRequestDto): ApiResult<PasteDto>
    
    /**
     * Получить заметку по ID
     */
    suspend fun getPaste(id: String): ApiResult<PasteDto>
    
    /**
     * Получить публичные заметки с пагинацией
     */
    suspend fun getPublicPastes(
        page: Int = 1, 
        limit: Int = 20
    ): ApiResult<List<PasteDto>>
    
    /**
     * Получить заметки пользователя (требует авторизации)
     */
    suspend fun getUserPastes(
        token: String,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<List<PasteDto>>
    
    /**
     * Удалить заметку (требует авторизации)
     */
    suspend fun deletePaste(token: String, id: String): ApiResult<DeleteResponseDto>
    
    // === Authentication ===
    
    /**
     * Регистрация нового пользователя
     */
    suspend fun register(request: RegisterRequestDto): ApiResult<AuthResponseDto>
    
    /**
     * Вход в систему
     */
    suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto>
    
    /**
     * Получить информацию о текущем пользователе
     */
    suspend fun getCurrentUser(token: String): ApiResult<UserDto>
    
    // === User Profile ===
    
    /**
     * Получить профиль пользователя по ID
     */
    suspend fun getUserProfile(userId: String): ApiResult<UserProfileDto>
    
    /**
     * Обновить свой профиль (требует авторизации)
     */
    suspend fun updateProfile(token: String, request: UpdateProfileRequestDto): ApiResult<UserDto>
    
    /**
     * Получить публичные заметки пользователя по ID
     */
    suspend fun getUserPublicPastes(
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<List<PasteDto>>
}

/**
 * Конфигурация API клиента
 */
data class ApiConfig(
    val baseUrl: String = "http://localhost:8080",
    val timeoutMs: Long = 30_000,
    val enableLogging: Boolean = true
)
