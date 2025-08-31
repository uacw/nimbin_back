# Nimbin Shared Module - Руководство разработчика

## Обзор

Shared модуль содержит общие DTO классы, константы и утилиты, которые используются как в backend (Ktor), так и в Android приложении. Это обеспечивает типобезопасность и консистентность данных между клиентом и сервером.

## Структура модуля

```
shared/src/main/kotlin/tech/nimbus/shared/
├── dto/                     # Data Transfer Objects
│   ├── PasteDto.kt         # Модель заметки
│   ├── UserDto.kt          # Модель пользователя
│   ├── PasteVisibility.kt  # Enum видимости заметок
│   ├── ResponseDtos.kt     # DTO для ответов API
│   └── request/
│       └── RequestDtos.kt  # DTO для запросов API
├── api/                    # API клиент и константы
│   ├── ApiConstants.kt     # Endpoints и заголовки
│   └── NimbinApiClient.kt  # Интерфейс API клиента
└── utils/                  # Утилиты
    ├── ApiResult.kt        # Sealed class для результатов API
    └── ValidationUtils.kt  # Валидация данных
```

## DTO Модели

### PasteDto
Основная модель текстовой заметки:

```kotlin
@Serializable
data class PasteDto(
    val id: String,
    val title: String,
    val content: String,
    val userId: String? = null,
    val authorUsername: String? = null,      // ✅ НОВОЕ: Username автора
    val authorDisplayName: String? = null,   // ✅ НОВОЕ: Отображаемое имя автора
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val createdAt: String,
    val expiresAt: String? = null,
    val language: String = "text",
    val viewCount: Int = 0
)
```

**Использование:**
- Backend: Конвертация из внутренних моделей в ответах API
- Android: Отображение заметок в UI, кеширование

**Особенности отображения автора в Android:**
```kotlin
// Рекомендуемая логика для UI
fun displayAuthorName(paste: PasteDto): String {
    return when {
        paste.authorDisplayName != null -> paste.authorDisplayName
        paste.authorUsername != null -> paste.authorUsername
        else -> "Anonymous"
    }
}

fun displayAuthorSecondary(paste: PasteDto): String? {
    return if (paste.authorDisplayName != null) "@${paste.authorUsername}" else null
}
```

### PasteVisibility
Enum для типов видимости заметок:

- `PUBLIC` - Публичная заметка (отображается в общем списке)
- `UNLISTED` - Скрытая заметка (доступна только по ссылке) 
- `PRIVATE` - Приватная заметка (доступна только владельцу)

**Использование в Android UI:**
```kotlin
fun getVisibilityIcon(visibility: PasteVisibility): ImageVector {
    return when (visibility) {
        PasteVisibility.PUBLIC -> Icons.Default.Public
        PasteVisibility.UNLISTED -> Icons.Default.LinkOff
        PasteVisibility.PRIVATE -> Icons.Default.Lock
    }
}
```

### UserDto
Модель пользователя для публичного API:

```kotlin
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String? = null,         // ✅ НОВОЕ: Отображаемое имя
    val email: String,
    val createdAt: String
)
```

**Примечание:** Не содержит приватные данные (пароль, соль и т.д.)

## Request DTOs

### CreatePasteRequestDto
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,          // ISO timestamp или null
    val language: String = "text"
)
```

**Валидация в Android:**
```kotlin
fun validateCreatePasteRequest(request: CreatePasteRequestDto): List<String> {
    val errors = mutableListOf<String>()
    
    if (request.title.isBlank()) errors.add("Title cannot be empty")
    if (request.title.length > 255) errors.add("Title too long (max 255 characters)")
    if (request.content.isBlank()) errors.add("Content cannot be empty")
    
    return errors
}
```

### RegisterRequestDto
```kotlin
@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)
```

### LoginRequestDto (⚠️ BREAKING CHANGE v2.1)
```kotlin
@Serializable
data class LoginRequestDto(
    val email: String,    // ✅ ИЗМЕНЕНО: теперь только email!
    val password: String
)
```

**Критическое изменение:** В версии 2.1 логин происходит только по email адресу, а не по username.

### UpdateProfileRequestDto (✅ НОВОЕ в v2.1)
```kotlin
@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)
```

**Использование:**
- Оба поля опциональны - можно обновлять по отдельности
- `null` значения игнорируются на сервере
- Валидация происходит на backend

## Response DTOs

### AuthResponseDto
```kotlin
@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto
)
```

**Использование в Android:**
```kotlin
// После успешной аутентификации
fun handleAuthSuccess(response: AuthResponseDto) {
    tokenManager.saveToken(response.token)
    userManager.saveUser(response.user)
    navigateToMainScreen()
}
```

### UserProfileDto (✅ НОВОЕ в v2.1)
```kotlin
@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int? = null    // null для чужих профилей
)
```

**Особенности:**
- `totalPastesCount` доступен только для собственного профиля
- Для чужих профилей всегда `null` по соображениям приватности

### ApiErrorDto
```kotlin
@Serializable
data class ApiErrorDto(
    val error: String
)
```

## API Client Interface

### NimbinApiClient
Интерфейс для реализации API клиента:

```kotlin
interface NimbinApiClient {
    suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto>
    suspend fun register(request: RegisterRequestDto): ApiResult<AuthResponseDto>
    suspend fun createPaste(request: CreatePasteRequestDto, token: String?): ApiResult<PasteDto>
    suspend fun getPaste(id: String, token: String?): ApiResult<PasteDto>
    suspend fun getPublicPastes(limit: Int, offset: Int): ApiResult<List<PasteDto>>
    suspend fun getMyPastes(token: String, visibility: String?): ApiResult<List<PasteDto>>
    suspend fun getUserProfile(token: String): ApiResult<UserProfileDto>
    suspend fun updateProfile(request: UpdateProfileRequestDto, token: String): ApiResult<UserDto>
}
```

## Утилиты

### ApiResult
Sealed class для типобезопасной обработки результатов API:

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
```

**Использование в Android:**
```kotlin
when (val result = apiClient.createPaste(request, token)) {
    is ApiResult.Success -> {
        // Успешное создание заметки
        val paste = result.data
        updateUI(paste)
    }
    is ApiResult.Error -> {
        // Обработка ошибки
        showError(result.message)
    }
}
```

### ValidationUtils
Утилиты для валидации данных на клиенте:

```kotlin
object ValidationUtils {
    fun isValidEmail(email: String): Boolean
    fun isValidUsername(username: String): Boolean
    fun isValidPassword(password: String): Boolean
}
```

## Интеграция с Android

### 1. Настройка зависимостей
```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation(project(":shared"))
    
    // Ktor Client для HTTP запросов
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    
    // Для JWT токен менеджмента
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

### 2. Реализация API клиента
```kotlin
class AndroidNimbinApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://nimbin-back-1de949af6629.herokuapp.com"
) : NimbinApiClient {
    
    override suspend fun createPaste(
        request: CreatePasteRequestDto,
        token: String?
    ): ApiResult<PasteDto> {
        return try {
            val response = httpClient.post("$baseUrl/api/pastes") {
                contentType(ContentType.Application.Json)
                setBody(request)
                token?.let { headers { append("Authorization", "Bearer $it") } }
            }
            ApiResult.Success(response.body<PasteDto>())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}
```

### 3. Repository Pattern
```kotlin
class PasteRepository(
    private val apiClient: NimbinApiClient,
    private val tokenManager: TokenManager
) {
    
    suspend fun createPaste(
        title: String,
        content: String,
        visibility: PasteVisibility
    ): Result<PasteDto> {
        val request = CreatePasteRequestDto(
            title = title,
            content = content,
            visibility = visibility
        )
        
        return when (val result = apiClient.createPaste(request, tokenManager.getToken())) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
```

### 4. ViewModel интеграция
```kotlin
class CreatePasteViewModel(
    private val pasteRepository: PasteRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreatePasteUiState())
    val uiState: StateFlow<CreatePasteUiState> = _uiState.asStateFlow()
    
    fun createPaste() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            pasteRepository.createPaste(
                title = _uiState.value.title,
                content = _uiState.value.content,
                visibility = _uiState.value.selectedVisibility
            ).fold(
                onSuccess = { paste ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}
```

## Миграция между версиями

### v2.0 → v2.1
**Критические изменения:**

1. **LoginRequestDto изменен:**
   ```kotlin
   // Старая версия
   LoginRequestDto(username = "user@example.com", password = "pass")
   
   // Новая версия
   LoginRequestDto(email = "user@example.com", password = "pass")
   ```

2. **Новые DTO:**
   - `UpdateProfileRequestDto` - для обновления профиля
   - `UserProfileDto` - расширенная информация профиля

3. **Обновленный PasteDto:**
   - Добавлены поля `authorUsername` и `authorDisplayName`

## Best Practices

### Обработка ошибок
```kotlin
// Централизованная обработка ошибок API
fun handleApiError(error: String): String {
    return when {
        error.contains("Token is not valid") -> "Session expired. Please login again."
        error.contains("User not found") -> "User not found"
        error.contains("Paste not found") -> "Paste not found"
        else -> error
    }
}
```

### Кеширование
```kotlin
// Кеширование заметок с учетом TTL
@Entity
data class CachedPaste(
    @PrimaryKey val id: String,
    val data: String, // JSON serialized PasteDto
    val cachedAt: Long,
    val ttl: Long = 5 * 60 * 1000L // 5 минут
)
```

### Безопасность токенов
```kotlin
// Использование EncryptedSharedPreferences для токенов
class SecureTokenManager(private val context: Context) {
    private val sharedPrefs = EncryptedSharedPreferences.create(
        "auth_tokens",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: String) {
        sharedPrefs.edit().putString("jwt_token", token).apply()
    }
}
```

---
*Обновлено: 31 августа 2025*
