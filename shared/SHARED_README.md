# Nimbin Shared DTO Module v2.1


Этот модуль содержит общие модели данных (DTO) для интеграции между Nimbin backend (Ktor) и клиентскими приложениями (Android/iOS).

## ⚠️ Критические изменения в v2.1

### LoginRequestDto изменен:
- **Было:** `username: String` (принимал username или email)
- **Стало:** `email: String` (только email адрес)

Это изменение требует обновления Android приложения.

## Структура модуля

```
shared/src/main/kotlin/tech/nimbus/shared/
├── dto/                          # Data Transfer Objects
│   ├── PasteDto.kt              # Основная модель заметки
│   ├── UserDto.kt               # Модель пользователя
│   ├── PasteVisibility.kt       # Enum видимости заметок
│   ├── ResponseDtos.kt          # DTO для ответов API
│   └── request/
│       └── RequestDtos.kt       # Login/Register/Profile/Создание заметки
└── ...
```

## Основные модели (актуально)

### PasteDto
```kotlin
@Serializable
data class PasteDto(
    val id: String,
    val title: String,
    val content: String,
    val userId: String? = null,
    val authorUsername: String? = null,
    val authorDisplayName: String? = null,
    val visibility: PasteVisibility = PUBLIC,
    val createdAt: String,
    val updatedAt: String,          // ✅ добавлено
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext",
    val viewCount: Int = 0,
    val etag: String? = null        // ✅ добавлено
)
```

### UserDto
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

### Request DTO

#### CreatePasteRequestDto
```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val visibility: PasteVisibility = PUBLIC,
    val expiresAt: String? = null,
    val syntaxLanguage: String = "plaintext"
)
```

#### LoginRequestDto (только email)
```kotlin
@Serializable
data class LoginRequestDto(
    val email: String,
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

## Использование ETag (оптимистичная блокировка)

- При получении заметки `GET /api/pastes/{id}` сервер возвращает заголовок `ETag: "<hash>"` и поле `etag` в `PasteDto`.
- При обновлении заметки необходимо передавать заголовок `If-Match: "<текущий-etag>"` в `PUT /api/pastes/{id}`.
- Ошибки:
  - `428 Precondition Required` — отсутствует `If-Match`.
  - `412 Precondition Failed` — ETag не совпадает (кто-то изменил заметку).

Рекомендуется сохранять `etag` вместе с заметкой в локальном кэше и использовать его при каждом редактировании.

## Использование в Android проекте

### 1. Подключение зависимости

В `settings.gradle.kts` проекта:
```kotlin
include(":shared")
project(":shared").projectDir = file("../Nimbin_back/shared")
```

В `build.gradle.kts` модуля приложения:
```kotlin
dependencies {
    implementation(project(":shared"))
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}
```

### 2. Клиент: пример обновления заметки с ETag

```kotlin
suspend fun updatePaste(
    httpClient: HttpClient,
    baseUrl: String,
    id: String,
    token: String,
    body: Any,
    etag: String
): PasteDto {
    return httpClient.put("$baseUrl/api/pastes/$id") {
        contentType(ContentType.Application.Json)
        setBody(body)
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
            append(HttpHeaders.IfMatch, "\"$etag\"")
        }
    }.body()
}
```

## Миграция с v2.0 на v2.1

- Перейти на логин по `email`.
- Обработать новые поля `updatedAt` и `etag` в `PasteDto`.
- При редактировании заметок использовать `If-Match`.

## Поддержка

При проблемах с интеграцией:
1. Проверьте версии Ktor (2.3.12+)
2. Проверьте подключение shared модуля
3. Сверьте форматы с API_DOCUMENTATION.md

---
