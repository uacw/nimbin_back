# Nimbin Shared DTO Module v2.1

**📅 Обновлено:** 25 августа 2025
**🚨 BREAKING CHANGES:** Изменена логика аутентификации

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
│       └── RequestDtos.kt       # ✅ ОБНОВЛЕНО: LoginRequestDto
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
    val visibility: PasteVisibility = PUBLIC,    // Видимость заметки
    val createdAt: String,                       // ISO timestamp
    val expiresAt: String? = null,               // Время удаления
    val language: String = "text",               // Язык для подсветки
    val viewCount: Int = 0                       // Счетчик просмотров
)
```

### Request DTO
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PUBLIC,
    val expiresInHours: Int? = null,
    val language: String = "text"
)

@Serializable
data class LoginRequestDto(
    val email: String, // Изменено: только email адрес
    val password: String
)
```

## Использование в Android проекте

### 1. Добавить зависимость в Android app

В `build.gradle.kts` вашего Android модуля:

```kotlin
dependencies {
    implementation(project(":shared"))
    
    // Для HTTP клиента (Ktor Client или Retrofit)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
}
```

### 2. Создать HTTP клиент

```kotlin
// ApiClient.kt в Android проекте
class AndroidNimbinApiClient(
    private val httpClient: HttpClient,
    private val config: ApiConfig = ApiConfig()
) : NimbinApiClient {
    
    override suspend fun createPaste(request: CreatePasteRequestDto): ApiResult<PasteDto> {
        return try {
            val response = httpClient.post("${config.baseUrl}${ApiEndpoints.PASTES}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ApiResult.Success(response.body<PasteDto>())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        return try {
            val response = httpClient.post("${config.baseUrl}${ApiEndpoints.LOGIN}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ApiResult.Success(response.body<AuthResponseDto>())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Login failed")
        }
    }
    
    // Реализация остальных методов...
}
```

### 3. Настроить DI (например, с Dagger Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideApiClient(httpClient: HttpClient): NimbinApiClient {
        return AndroidNimbinApiClient(httpClient)
    }
}
```

### 4. Использовать в Repository/ViewModel

```kotlin
class PasteRepository @Inject constructor(
    private val apiClient: NimbinApiClient
) {
    
    suspend fun createPaste(
        title: String,
        content: String,
        language: String = "text"
    ): ApiResult<PasteDto> {
        val request = CreatePasteRequestDto(
            title = title,
            content = content,
            language = language
        )
        
        return apiClient.createPaste(request)
    }
    
    suspend fun getPublicPastes(page: Int = 0): ApiResult<PaginatedPastesResponseDto> {
        return apiClient.getPublicPastes(page = page, size = 20)
    }
}
```

## Валидация данных

Используйте `ValidationUtils` для проверки данных перед отправкой:

```kotlin
fun validatePasteData(title: String, content: String): List<String> {
    val errors = mutableListOf<String>()
    
    if (!ValidationUtils.isValidTitle(title)) {
        errors.add("Title must be 1-255 characters")
    }
    
    if (!ValidationUtils.isValidContent(content)) {
        errors.add("Content must be 1-1,000,000 characters")
    }
    
    return errors
}
```

## Обработка результатов API

```kotlin
// В ViewModel
viewModelScope.launch {
    apiClient.createPaste(request)
        .onSuccess { paste ->
            _pasteState.value = PasteState.Success(paste)
        }
        .onError { message, code ->
            _pasteState.value = PasteState.Error(message)
        }
}
```

## Конфигурация для разных окружений

```kotlin
// Для разработки
val devConfig = ApiConfig(
    baseUrl = "http://10.0.2.2:8080", // Android эмулятор
    enableLogging = true
)

// Для продакшна
val prodConfig = ApiConfig(
    baseUrl = "https://api.nimbin.tech",
    enableLogging = false
)
```

## Поддерживаемые операции

- ✅ Создание заметок (анонимно и с авторизацией)
- ✅ Получение заметки по ID
- ✅ Список публичных заметок с пагинацией
- ✅ Список заметок пользователя (требует JWT)
- ✅ Удаление заметок (только владелец)
- ✅ Регистрация и логин пользователей
- ✅ Получение информации о текущем пользователе

## Миграция с мультиплатформы

В будущем этот модуль можно легко конвертировать в Kotlin Multiplatform для поддержки iOS:

1. Изменить `kotlin("jvm")` на `kotlin("multiplatform")` в build.gradle.kts
2. Добавить iOS таргеты
3. Реализовать платформоспецифичные HTTP клиенты

Все DTO модели уже готовы для multiplatform использования!
