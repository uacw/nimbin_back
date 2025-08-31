# Nimbin Shared Module - Руководство разработчика

## Обзор

Shared модуль содержит общие DTO классы, константы и утилиты, которые используются как в backend (Ktor), так и в Android приложении. Это обеспечивает типобезопасность и консистентность данных между клиентом и сервером.

## Структура модуля

```
shared/src/commonMain/kotlin/tech/nimbus/shared/
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
    val authorUsername: String? = null,      // ✅ НОВОЕ
    val authorDisplayName: String? = null,   // ✅ НОВОЕ
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

### PasteVisibility
Enum для типов видимости заметок:

- `PUBLIC` - Публичная заметка (отображается в общем списке)
- `UNLISTED` - Скрытая заметка (доступна только по ссылке) 
- `PRIVATE` - Приватная заметка (доступна только владельцу)

### UserDto
Модель пользователя для публичного API:

```kotlin
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String? = null,         // ✅ НОВОЕ
    val email: String,
    val createdAt: String
)
```

**Примечание:** Не содержит приватные данные (пароль, соль и т.д.)

## Request/Response DTOs

### Запросы к API

#### CreatePasteRequestDto
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null,
    val language: String = "text"
)
```

#### RegisterRequestDto
```kotlin
@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)
```

#### LoginRequestDto
```kotlin
@Serializable
data class LoginRequestDto(
    val username: String,    // Может быть username или email
    val password: String
)

/** ✅ НОВОЕ **/
@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)
```

### Ответы API

#### AuthResponseDto
```kotlin
@Serializable
data class AuthResponseDto(
    val token: String,       // JWT токен
    val user: UserDto       // Информация о пользователе
)
```

#### ApiErrorDto
```kotlin
@Serializable
data class ApiErrorDto(
    val error: String,
    val code: String? = null
)

/** ✅ НОВОЕ **/
@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int? = null
)
```

## API клиент и константы

### ApiEndpoints
Объект с константами всех API endpoints:

```kotlin
object ApiEndpoints {
    const val API_BASE = "/api"
    const val PASTES = "/api/pastes"
    // ...
    const val LOGIN = "/api/auth/login"
    
    // ✅ НОВЫЕ User endpoints
    const val USER_BASE = "$API_BASE/users"
    const val USER_PROFILE = "$USER_BASE/profile"      // GET (свой), PUT (обновить)
    const val USER_BY_ID = "$USER_BASE/{id}"           // GET (чужой)
    const val USER_PUBLIC_PASTES = "$USER_BASE/{id}/pastes" // GET (заметки чужого)
    
    // Helper методы
    fun pasteById(id: String): String
    fun publicPastes(page: Int, limit: Int): String
    fun userPastes(page: Int, limit: Int): String

    // ✅ НОВЫЕ Helper методы
    fun userProfileById(userId: String): String
    fun userPublicPastes(userId: String, page: Int, limit: Int): String
}
```

### NimbinApiClient (Интерфейс)
Определяет контракт API клиента для реализации на каждой платформе:

**Основные методы:**
- `createPaste(request)` - Создание заметки
- `getPaste(id)` - Получение заметки по ID
- `getPublicPastes(page, limit)` - Публичные заметки
- `getUserPastes(token, page, limit)` - Заметки пользователя
- `deletePaste(token, id)` - Удаление заметки
- `register(request)` - Регистрация
- `login(request)` - Вход
- `getCurrentUser(token)` - Текущий пользователь

// ✅ НОВЫЕ методы
- `getUserProfile(userId)` - Получить профиль пользователя
- `updateProfile(token, request)` - Обновить свой профиль
- `getUserPublicPastes(userId, page, limit)` - Публичные заметки пользователя
```

## Утилиты

### ApiResult<T>
Sealed class для безопасной обработки результатов API:

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    
    // Helper методы
    fun getOrNull(): T?
    fun getOrDefault(defaultValue: T): T
    fun onSuccess(action: (T) -> Unit): ApiResult<T>
    fun onError(action: (String, Int?) -> Unit): ApiResult<T>
}
```

**Использование в Android:**
```kotlin
when (val result = apiClient.getPaste(id)) {
    is ApiResult.Success -> showPaste(result.data)
    is ApiResult.Error -> showError(result.message)
}
```

### ValidationUtils
Константы и методы для валидации данных:

**Константы:**
- `MIN_TITLE_LENGTH = 1`, `MAX_TITLE_LENGTH = 255`
- `MIN_CONTENT_LENGTH = 1`, `MAX_CONTENT_LENGTH = 1_000_000`
- `PASTE_ID_LENGTH = 12`
- `MIN_USERNAME_LENGTH = 3`, `MAX_USERNAME_LENGTH = 50`
- `MIN_PASSWORD_LENGTH = 6`, `MAX_PASSWORD_LENGTH = 128`
- `SUPPORTED_LANGUAGES` - Set поддерживаемых языков

**Методы валидации:**
- `isValidTitle(title: String): Boolean`
- `isValidContent(content: String): Boolean`
- `isValidPasteId(id: String): Boolean`
- `isValidUsername(username: String): Boolean`
- `isValidEmail(email: String): Boolean`
- `isValidPassword(password: String): Boolean`
```

## Подключение в проектах

### Backend (build.gradle.kts)
```kotlin
dependencies {
    implementation(project(":shared"))
    // ... другие зависимости
}
```

### Android (build.gradle)
```kotlin
dependencies {
    implementation project(':shared')
    // ... другие зависимости
}
```

## Принципы использования

### 1. Типобезопасность
Все DTO используют строгую типизацию и kotlinx.serialization

### 2. Неизменяемость
Все DTO классы являются data классами с immutable свойствами

### 3. Документированность
Каждый класс и важные свойства имеют KDoc комментарии

### 4. Валидация
Используйте ValidationUtils для проверки данных перед отправкой запросов

### 5. Обработка ошибок
Всегда используйте ApiResult для обработки результатов API операций

## Примеры использования в Android

### Создание заметки
```kotlin
val request = CreatePasteRequestDto(
    title = "My Note",
    content = "Hello World",
    visibility = PasteVisibility.PUBLIC,
    language = "kotlin"
)

// ...
```

### Аутентификация
```kotlin
val loginRequest = LoginRequestDto("username", "password")
val result = apiClient.login(loginRequest)

result.onSuccess { authResponse ->
    // Сохранить токен: authResponse.token
    // Получить пользователя: authResponse.user
}.onError { error, code ->
    // Показать ошибку входа
}
```

### Загрузка заметок
```kotlin
// Публичные заметки
val publicResult = apiClient.getPublicPastes(page = 1, limit = 20)

// Заметки пользователя (требует токен)  
val userResult = apiClient.getUserPastes(token, page = 1, limit = 20)

// ✅ НОВЫЙ пример: Публичные заметки другого пользователя
val otherUserPastes = apiClient.getUserPublicPastes(userId = "some-user-id")
```

## Версионирование

При внесении изменений в shared модель:

1. **Breaking changes** - увеличить major версию
2. **Новые поля** - добавлять с default значениями
3. **Удаление полей** - помечать @Deprecated перед удалением

## Рекомендации

1. **Валидация на клиенте** - используйте ValidationUtils перед отправкой
2. **Обработка ошибок** - всегда обрабатывайте ApiResult.Error
3. **Кэширование** - DTO можно безопасно сериализовать для кеша
4. **Тестирование** - создавайте mock данные используя DTO из shared модуля
5. **Отображение автора** - используйте `authorDisplayName` или `authorUsername` из `PasteDto`

---

**Версия:** 2.0  
**Последнее обновление:** 20.08.2025  
**Совместимость:** Backend v2.0, Android TBD
