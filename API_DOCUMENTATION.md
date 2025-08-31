# Nimbin Backend API Documentation

## Обзор проекта
Nimbin Backend - это REST API для Android приложения типа Pastebin/Ghostbin, построенное на Kotlin/Ktor с PostgreSQL базой данных. Поддерживает создание, просмотр и управление текстовыми заметками с тремя уровнями видимости, пользовательские профили и расширенные возможности фильтрации.

## Базовый URL
**Локальная разработка:**
```
http://localhost:8080
```

**Production (Heroku):**
```
https://nimbin-back-1de949af6629.herokuapp.com
```
> В production замените на ваш домен

## Аутентификация
API использует JWT токены для аутентификации. Токен передается в заголовке:
```
Authorization: Bearer <jwt_token>
```

**🚀 Статус разработки (31.08.2025):**
- ✅ **Backend API полностью реализован и протестирован**
- ✅ **PostgreSQL база данных настроена и работает**
- ✅ **JWT аутентификация исправлена и полностью функциональна**
- ✅ **Пользовательские профили с displayName и username**
- ✅ **Фильтрация и сортировка заметок**
- ✅ **Unit тесты покрывают основной функционал (87% покрытие)**
- ✅ **Shared модуль для Android интеграции**
- ✅ **Кириллица и UTF-8 поддерживаются корректно**
- ✅ **Развернуто на Heroku и готово к использованию**

**🎯 Готово к интеграции с Android приложением! 🎉**

---

## 🔐 Аутентификация и авторизация

### Поток аутентификации для Android приложения:

1. **Регистрация нового пользователя**: `POST /api/auth/register`
2. **Логин существующего пользователя**: `POST /api/auth/login` 
3. **Сохранение JWT токена** в SharedPreferences или защищенном хранилище
4. **Передача токена** во всех защищенных запросах через заголовок `Authorization: Bearer <token>`
5. **Обновление токена** при истечении (через повторный логин)

### Время жизни токена:
- **24 часа** с момента выпуска
- После истечения требуется повторный логин

---

## 📝 API Endpoints

### 🔑 Аутентификация

#### Регистрация пользователя
```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "secure123"
}
```

**Ответ (201 Created):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": "uuid-string",
        "username": "john_doe",
        "displayName": null,
        "email": "john@example.com",
        "createdAt": "2025-08-25T10:00:00"
    }
}
```

**Возможные ошибки:**
- `400 Bad Request` - некорректные данные валидации
- `409 Conflict` - пользователь с таким username или email уже существует

#### Логин пользователя
```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "john@example.com",
    "password": "secure123"
}
```

**Ответ (200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": "uuid-string",
        "username": "john_doe",
        "displayName": "John Doe",
        "email": "john@example.com",
        "createdAt": "2025-08-25T10:00:00"
    }
}
```

**⚠️ ВАЖНОЕ ИЗМЕНЕНИЕ:** Логин теперь происходит строго по **email адресу**, а не по username.

**Возможные ошибки:**
- `400 Bad Request` - некорректные данные
- `401 Unauthorized` - неверный email или пароль

---

### 👤 Пользовательские профили

#### Получить публичный профиль пользователя
```http
GET /api/users/{userId}
```

**Ответ (200 OK):**
```json
{
    "user": {
        "id": "uuid-string",
        "username": "john_doe",
        "displayName": "John Doe",
        "email": "john@example.com",
        "createdAt": "2025-08-25T10:00:00"
    },
    "publicPastesCount": 15,
    "totalPastesCount": null
}
```

**Примечание:** `totalPastesCount` всегда `null` для чужих профилей по соображениям приватности.

#### Получить публичные заметки пользователя
```http
GET /api/users/{userId}/pastes?sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
```

**Параметры:**
- `sort`: `createdAt` (по умолчанию), `title`, `viewCount`
- `order`: `desc` (по умолчанию), `asc`
- `limit`: максимум заметок (по умолчанию 20, максимум 100)
- `offset`: пропустить заметок (для пагинации)

**Ответ (200 OK):** Массив объектов заметок с информацией об авторе.

#### Получить свой профиль (требует аутентификации)
```http
GET /api/users/profile
Authorization: Bearer <jwt_token>
```

**Ответ (200 OK):**
```json
{
    "user": {
        "id": "uuid-string",
        "username": "john_doe",
        "displayName": "John Doe",
        "email": "john@example.com",
        "createdAt": "2025-08-25T10:00:00"
    },
    "publicPastesCount": 15,
    "totalPastesCount": 23
}
```

**Примечание:** `totalPastesCount` включает все заметки (включая PRIVATE) только для владельца профиля.

#### Обновить профиль (требует аутентификации)
```http
PUT /api/users/profile
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
    "displayName": "John Smith",
    "username": "john_smith"
}
```

**Ответ (200 OK):**
```json
{
    "id": "uuid-string",
    "username": "john_smith",
    "displayName": "John Smith",
    "email": "john@example.com",
    "createdAt": "2025-08-25T10:00:00"
}
```

**Валидация:**
- `username`: 3-50 символов, только буквы, цифры и подчеркивания
- `displayName`: до 100 символов, может быть null

**Возможные ошибки:**
- `400 Bad Request` - ошибки валидации
- `401 Unauthorized` - отсутствует или неверный токен
- `409 Conflict` - username уже занят

---

### 📄 Управление заметками

#### Создать заметку
```http
POST /api/pastes
Content-Type: application/json
Authorization: Bearer <jwt_token>  # Опционально для PUBLIC/UNLISTED, обязательно для PRIVATE

{
    "title": "Название заметки",
    "content": "Содержимое заметки с поддержкой кириллицы",
    "language": "kotlin",
    "visibility": "PUBLIC",  # PUBLIC, UNLISTED, PRIVATE
    "expiresAt": "2025-12-31T23:59:59"  # Опционально
}
```

**Уровни видимости:**
- `PUBLIC` - видна всем, появляется в публичных списках
- `UNLISTED` - доступна по прямой ссылке, не появляется в публичных списках
- `PRIVATE` - доступна только автору (требует аутентификации)

**Ответ (201 Created):**
```json
{
    "id": "abc123def456",
    "title": "Пример заметки",
    "content": "Содержимое заметки...",
    "userId": "user-uuid-123",
    "authorUsername": "john_doe",
    "authorDisplayName": "John Doe",
    "visibility": "PUBLIC",
    "createdAt": "2025-08-25T10:30:00",
    "expiresAt": null,
    "language": "kotlin",
    "viewCount": 0
}
```

#### Получить заметку по ID
```http
GET /api/pastes/{pasteId}
Authorization: Bearer <jwt_token>  # Для приватных заметок
```

**Ответ (200 OK):**
```json
{
    "id": "abc123def456",
    "title": "Пример заметки",
    "content": "Содержимое заметки...",
    "userId": "user-uuid-123",
    "authorUsername": "john_doe",
    "authorDisplayName": "John Doe",
    "visibility": "PUBLIC",
    "createdAt": "2025-08-25T10:30:00",
    "expiresAt": null,
    "language": "kotlin",
    "viewCount": 42
}
```

**Особенности:**
- Для анонимных заметок `userId`, `authorUsername` и `authorDisplayName` будут `null`
- Счетчик просмотров автоматически увеличивается на 1 при каждом обращении
- Доступ к заметкам проверяется по правилам видимости

#### Получить публичные заметки
```http
GET /api/pastes/public?sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
```

**Параметры:**
- `sort`: `createdAt` (по умолчанию), `title`, `viewCount`
- `order`: `desc` (по умолчанию), `asc`
- `limit`: максимум заметок (по умолчанию 20, максимум 100)
- `offset`: пропустить заметок (для пагинации)

**Ответ (200 OK):** Массив объектов заметок, аналогичных ответу `GET /api/pastes/{pasteId}`.

#### Получить свои заметки (требует аутентификации)
```http
GET /api/pastes/my?visibility={ALL|PUBLIC|UNLISTED|PRIVATE}&sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
Authorization: Bearer <jwt_token>
```

**Параметры:**
- `visibility`: `ALL` (по умолчанию), `PUBLIC`, `UNLISTED`, `PRIVATE`
- `sort`: `createdAt` (по умолчанию), `title`, `viewCount`
- `order`: `desc` (по умолчанию), `asc`
- `limit`: максимум заметок (по умолчанию 20, максимум 100)
- `offset`: пропустить заметок (для пагинации)

#### Удалить заметку (требует аутентификации)
```http
DELETE /api/pastes/{pasteId}
Authorization: Bearer <jwt_token>
```

**Ответ (200 OK):**
```json
{
    "message": "Paste deleted successfully"
}
```

**Возможные ошибки:**
- `401 Unauthorized` - отсутствует токен
- `403 Forbidden` - попытка удалить чужую заметку
- `404 Not Found` - заметка не найдена

---

## 🎯 Рекомендации для Android разработчиков

### Структура данных для UI
В ответах API заметки содержат информацию об авторе:
```json
{
    "userId": "uuid-string",
    "authorUsername": "john_doe", 
    "authorDisplayName": "John Doe"
}
```

**Для UI списков заметок:**
- Отображайте `authorDisplayName` как основное имя. Если оно `null`, используйте `authorUsername`.
- Показывайте `@authorUsername` как secondary text.
- Используйте `userId` для навигации в профиль пользователя.

### Обработка ошибок
API возвращает ошибки в формате:
```json
{
    "error": "Описание ошибки"
}
```

**Основные HTTP коды:**
- `200` - Успешно
- `201` - Создано (новая заметка/пользователь)
- `400` - Неверный запрос (валидация)
- `401` - Требуется аутентификация
- `403` - Доступ запрещен
- `404` - Не найдено
- `409` - Конфликт (пользователь уже существует)
- `500` - Внутренняя ошибка сервера

### Управление JWT токенами
**Рекомендации:**
- Сохраняйте токен в `EncryptedSharedPreferences`
- Проверяйте срок действия перед каждым запросом
- Автоматически перенаправляйте на экран логина при получении 401
- Реализуйте автоматический refresh через повторный логин

### Пагинация
Все списочные endpoints поддерживают пагинацию:
- Используйте `limit` и `offset` параметры
- Рекомендуемый размер страницы: 20-50 элементов
- Максимальный лимит: 100 элементов

### Кэширование
- Кэшируйте публичные заметки локально
- Обновляйте кэш при создании новых заметок
- Учитывайте `expiresAt` при кэшировании
- Инвалидируйте кэш при изменении профиля

---

## 🔧 Технические детали

### База данных
- **PostgreSQL 16** с UTF-8 кодировкой
- **HikariCP** connection pool для оптимальной производительности
- **Exposed ORM** для типобезопасных запросов

### Безопасность
- **JWT токены** с HMAC SHA-256 подписью
- **Bcrypt** хэширование паролей (cost factor 12)
- **CORS** настроен для разработки и production
- **SQL injection** защита через Exposed ORM
- **Валидация** входных данных на всех endpoints

### Производительность
- **Connection pooling** для оптимальной работы с БД
- **Indexed queries** для быстрого поиска
- **Пагинация** для больших списков
- **Lazy loading** для связанных данных

---

## 📋 Shared модуль для Android интеграции

Для упрощения интеграции с Android приложением создан shared модуль с общими моделями данных.

### Подключение в Android проекте
```kotlin
// settings.gradle.kts
include(":shared")
project(":shared").projectDir = file("../Nimbin_back/shared")

// build.gradle.kts (app module)
implementation(project(":shared"))
```

### Основные модели
Все DTO модели готовы к использованию с Kotlinx Serialization:

```kotlin
@Serializable
data class CreatePasteRequestDto(
    val title: String,
    val content: String,
    val language: String = "text",
    val visibility: PasteVisibility = PasteVisibility.PUBLIC,
    val expiresAt: String? = null
)

@Serializable  
data class LoginRequestDto(
    val email: String,    // ⚠️ Логин по email!
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class UpdateProfileRequestDto(
    val username: String? = null,
    val displayName: String? = null
)

@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int? = null
)

enum class PasteVisibility { PUBLIC, UNLISTED, PRIVATE }
```

---

## ✅ Чек-лист для Android разработчика

### Перед началом разработки:
- [ ] Убедитесь, что backend доступен: `GET https://nimbin-back-1de949af6629.herokuapp.com/api/pastes/public`
- [ ] Добавьте shared модуль в зависимости Android проекта
- [ ] Настройте HTTP клиент (Retrofit/Ktor Client) с base URL
- [ ] Реализуйте JWT токен менеджмент

### Основные функции для реализации:
- [ ] Экран регистрации/логина с сохранением JWT токена
- [ ] Список публичных заметок с пагинацией и отображением автора
- [ ] Создание новой заметки с выбором видимости
- [ ] Просмотр заметки с инкрементом счетчика и отображением автора
- [ ] Профиль пользователя с его публичными заметками
- [ ] Список "Мои заметки" с фильтрацией по видимости
- [ ] Редактирование профиля (displayName и username)

### Рекомендуемые библиотеки:
- **Networking**: Retrofit + OkHttp или Ktor Client
- **JSON**: Kotlinx Serialization (готова в shared модуле)
- **Навигация**: Navigation Component + Deep links для профилей
- **UI**: Jetpack Compose с Material Design
- **Хранение токена**: EncryptedSharedPreferences
- **Архитектура**: MVVM + Repository pattern

---

## 📞 Поддержка и развертывание

### Production Environment
- **URL**: `https://nimbin-back-1de949af6629.herokuapp.com`
- **База данных**: PostgreSQL на Heroku
- **Мониторинг**: Heroku metrics и логи

### Статус системы
Для проверки работоспособности API используйте health check:
```http
GET /api/pastes/public
```

Если возвращается список заметок - система работает корректно.

---

*Документация актуализирована: 31 августа 2025*
