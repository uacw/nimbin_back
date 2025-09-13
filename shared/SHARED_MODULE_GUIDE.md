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
    val authorUsername: String? = null,      // Username автора
    val authorDisplayName: String? = null,   // Отображаемое имя автора
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val createdAt: String,
    val updatedAt: String,                   // ✅ время последнего обновления
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext",
    val viewCount: Int = 0,
    val etag: String? = null,                // ✅ ETag для If-Match
    val isFavorite: Boolean? = null          // ✅ признак избранного для текущего пользователя
)
```

**Использование:**
- Backend: конвертация из внутренних моделей в ответах API
- Android: отображение заметок в UI, кеширование, работа с ETag и флагом избранного

### PasteVisibility
Enum для типов видимости заметок:

- `PUBLIC` — Публичная заметка (отображается в общем списке)
- `UNLISTED` — Скрытая заметка (доступна только по ссылке)
- `PRIVATE` — Приватная заметка (доступна только владельцу)

### UserDto
Модель пользователя для публичного API:

```kotlin
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val email: String,
    val createdAt: String
)
```

## Request DTOs

### CreatePasteRequestDto
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,          // ISO timestamp или null
    val syntaxLanguage: String = "plaintext"
)
```

### UpdatePasteRequestDto (частичное обновление)
```kotlin
@Serializable
data class UpdatePasteRequestDto(
    val title: String? = null,
    val content: String? = null,
    val syntaxLanguage: String? = null,
    val visibility: PasteVisibility? = null,
    val expiresAt: String? = null
)
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
    val email: String,    // теперь только email
    val password: String
)
```

### UpdateProfileRequestDto
```kotlin
@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)
```

## Response DTOs

### AuthResponseDto
```kotlin
@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto
)
```

### UserProfileDto
```kotlin
@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int? = null
)
```

## API: избранное и ETag

- ETag: `GET /api/pastes/{id}` возвращает заголовок `ETag` и поле `etag` в `PasteDto`. Для `PUT /api/pastes/{id}` используйте `If-Match`.
- Избранное:
  - `POST /api/pastes/{id}/favorite` — добавить в избранное
  - `DELETE /api/pastes/{id}/favorite` — удалить из избранного
  - `GET /api/pastes/my?favorite=true` — только избранные заметки пользователя
  - Поле `isFavorite` в `PasteDto` будет проставлено при запросах с токеном

## API Client Interface (пример)

```kotlin
interface NimbinApiClient {
    suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto>
    suspend fun register(request: RegisterRequestDto): ApiResult<AuthResponseDto>
    suspend fun createPaste(request: CreatePasteRequestDto, token: String?): ApiResult<PasteDto>
    suspend fun getPaste(id: String, token: String?): ApiResult<PasteDto>
    suspend fun getPublicPastes(limit: Int, offset: Int, token: String?): ApiResult<List<PasteDto>>
    suspend fun getMyPastes(token: String, favoriteOnly: Boolean = false): ApiResult<List<PasteDto>>
    suspend fun updatePaste(id: String, body: UpdatePasteRequestDto, token: String, etag: String): ApiResult<PasteDto>
    suspend fun addFavorite(id: String, token: String): ApiResult<Unit>
    suspend fun removeFavorite(id: String, token: String): ApiResult<Unit>
}
```

## Утилиты

### ApiResult
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
```

## Интеграция с Android

### Зависимости
```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation(project(":shared"))
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}
```
