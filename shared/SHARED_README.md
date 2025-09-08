# Nimbin Shared DTO Module v2.1


Этот модуль содержит общие модели данных (DTO) для интеграции между Nimbin backend (Ktor) и клиентскими приложениями (Android/iOS).

## ⚠️ Критические изменения в v2.1

### LoginRequestDto изменен:
- **Было:** `username: String` (принимал username или email)
- **Стало:** `email: String` (только email адрес)

Это изменение требует обновления Android приложения!

## Структура модуля

```
shared/src/main/kotlin/tech/nimbus/shared/
├── dto/                          # Data Transfer Objects
│   ├── PasteDto.kt              # Основная модель заметки
│   ├── UserDto.kt               # Модель пользователя
│   ├── PasteVisibility.kt       # Enum видимости заметок
│   ├── ResponseDtos.kt          # DTO для ответов API
│   └── request/
│       └── RequestDtos.kt       # ✅ ОБНОВЛЕНО: LoginRequestDto + новые DTO
├── api/                         # API интерфейсы и константы
│   ├── NimbinApiClient.kt       # Интерфейс API клиента
│   └── ApiConstants.kt          # Endpoints и заголовки
└── utils/                       # Утилиты
    ├── ValidationUtils.kt       # Валидация данных
    └── ApiResult.kt            # Обработка результатов API
```

## Основные модели

### PasteDto
```kotlin
@Serializable
data class PasteDto(
    val id: String,                              // Уникальный 12-символьный ID
    val title: String,                           // Заголовок заметки
    val content: String,                         // Содержимое
    val userId: String? = null,                  // ID владельца (null для анонимных)
    val authorUsername: String? = null,          // Username автора
    val authorDisplayName: String? = null,       // Отображаемое имя автора
    val visibility: PasteVisibility = PUBLIC,    // Видимость заметки
    val createdAt: String,                       // ISO timestamp
    val expiresAt: String? = null,               // Время удаления (ISO timestamp)
    val syntaxLanguage: String = "plaintext",   // Язык для подсветки
    val viewCount: Int = 0                       // Счетчик просмотров
)
```

### UserDto
```kotlin
@Serializable
data class UserDto(
    val id: String,                    // UUID пользователя
    val username: String,              // Уникальное имя пользователя
    val displayName: String? = null,   // ✅ НОВОЕ: Отображаемое имя
    val email: String,                 // Email адрес
    val createdAt: String             // Дата регистрации (ISO timestamp)
)
```

### Request DTO

#### CreatePasteRequestDto
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PUBLIC,
    val expiresAt: String? = null,               // ✅ ИСПРАВЛЕНО: String вместо expiresInHours
    val syntaxLanguage: String = "plaintext"
)
```

#### LoginRequestDto (⚠️ BREAKING CHANGE)
```kotlin
@Serializable
data class LoginRequestDto(
    val email: String,    // ✅ ИЗМЕНЕНО: только email адрес!
    val password: String
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

#### UpdateProfileRequestDto
```kotlin
@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)
```

## Использование в Android проекте

### 1. Добавить зависимость в Android app

В `settings.gradle.kts` проекта:
```kotlin
include(":shared")
project(":shared").projectDir = file("../Nimbin_back/shared")
```

В `build.gradle.kts` вашего Android модуля:
```kotlin
dependencies {
    implementation(project(":shared"))
    
    // Для HTTP клиента (обновленные версии)
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}
```

### 2. Создать HTTP клиент

```kotlin
// ApiClient.kt в Android проекте
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
                token?.let {
                    headers {
                        append("Authorization", "Bearer $it")
                    }
                }
            }
            ApiResult.Success(response.body<PasteDto>())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ApiResult.Success(response.body<AuthResponseDto>())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Login failed")
        }
    }
}
```

### 3. Использование в ViewModel

```kotlin
class PastesViewModel(
    private val apiClient: NimbinApiClient,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    fun createPaste(title: String, content: String, visibility: PasteVisibility) {
        viewModelScope.launch {
            val request = CreatePasteRequestDto(
                title = title,
                content = content,
                visibility = visibility,
                syntaxLanguage = "kotlin"
            )
            
            val result = apiClient.createPaste(request, tokenManager.getToken())
            when (result) {
                is ApiResult.Success -> {
                    // Paste создан успешно
                    _pastesState.value = _pastesState.value.copy(
                        pastes = listOf(result.data) + _pastesState.value.pastes
                    )
                }
                is ApiResult.Error -> {
                    // Обработка ошибки
                    _errorState.value = result.message
                }
            }
        }
    }
}
```

## Миграция с v2.0 на v2.1

### Критические изменения в коде:

**До (v2.0):**
```kotlin
// Старый способ логина
val loginRequest = LoginRequestDto(
    username = "user@example.com",  // Работал и username и email
    password = "password"
)
```

**После (v2.1):**
```kotlin
// Новый способ логина - только email!
val loginRequest = LoginRequestDto(
    email = "user@example.com",     // Только email адрес
    password = "password"
)
```

### Обновление Android приложения:
1. Измените все экраны логина для использования email вместо username
2. Обновите валидацию - проверяйте email формат
3. Обновите сохраненные данные пользователей
4. Протестируйте аутентификацию с новой логикой

## Версионирование

- **v2.0** - Первая стабильная версия с базовым функционалом
- **v2.1** - Breaking changes: login по email + новые профильные DTO
- **v2.2** - Планируется: добавление endpoints для управления заметками

## Поддержка

При возникновении проблем с интеграцией shared модуля:
1. Убедитесь, что используете корректные версии Ktor (2.3.12+)
2. Проверьте правильность подключения модуля в settings.gradle.kts
3. Обратитесь к API_DOCUMENTATION.md для актуальных примеров запросов

---
