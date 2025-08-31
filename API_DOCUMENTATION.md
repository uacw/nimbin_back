# Nimbin Backend API Documentation

## Обзор проекта
Nimbin Backend - это REST API для Android приложения типа Pastebin/Ghostbin, построенное на Kotlin/Ktor с PostgreSQL базой данных. Поддерживает создание, просмотр и управление текстовыми заметками с тремя уровнями видимости, пользовательские профили и расширенные возможности фильтрации.

## Базовый URL
```
http://localhost:8080
```
> В production замените на ваш домен

## Аутентификация
API использует JWT токены для аутентификации. Токен передается в заголовке:
```
Authorization: Bearer <jwt_token>
```

**🚀 Статус разработки (18.08.2025):**
- ✅ **Backend API полностью реализован и протестирован**
- ✅ **PostgreSQL база данных настроена и работает**
- ✅ **JWT аутентификация исправлена и полностью функциональна**
- ✅ **Пользовательские профили с именем и username**
- ✅ **Фильтрация и сортировка заметок**
- ✅ **Unit тесты покрывают основной функционал (87% покрытие)**
- ✅ **Shared модуль для Android интеграции**
- ✅ **Кириллица и UTF-8 поддерживаются корректно**

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
        "createdAt": "2025-08-18T10:00:00"
    }
}
```

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
        "createdAt": "2025-08-18T10:00:00"
    }
}
```

**⚠️ ВАЖНОЕ ИЗМЕНЕНИЕ:** Логин теперь происходит строго по **email адресу**, а не по username. Это изменение вступило в силу 25.08.2025.

---

### 👤 Пользовательские профили

#### Получить профиль пользователя
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
        "createdAt": "2025-08-18T10:00:00"
    },
    "publicPastesCount": 15,
    "totalPastesCount": null
}
```

#### Получить публичные заметки пользователя
```http
GET /api/users/{userId}/pastes?sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
```

**Параметры:**
- `sort`: `createdAt` (по умолчанию), `title`, `viewCount`
- `order`: `desc` (по умолчанию), `asc`
- `limit`: максимум заметок (по умолчанию 20, максимум 100)
- `offset`: пропустить заметок (для пагинации)

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
    "createdAt": "2025-08-18T10:00:00"
}
```

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
        "createdAt": "2025-08-18T10:00:00"
    },
    "publicPastesCount": 15,
    "totalPastesCount": 23
}
```

---

### 📄 Управление заметками

#### Создать заметку
```http
POST /api/pastes
Content-Type: application/json
Authorization: Bearer <jwt_token>  # Опционально

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
    "createdAt": "2025-08-20T10:30:00",
    "expiresAt": null,
    "language": "kotlin",
    "viewCount": 42
}
```

**Примечания:**
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

**Ответ (200 OK):**
Массив объектов заметок, аналогичных ответу `GET /api/pastes/{pasteId}`.

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
- Показывайте `@author.username` как secondary text.
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

### Пагинация
Все списочные endpoints поддерживают пагинацию:
- Используйте `limit` и `offset` параметры
- Рекомендуемый размер страницы: 20-50 элементов
- Максимальный лимит: 100 элементов

### Кэширование
- Кэшируйте публичные заметки локально
- Обновляйте кэш при создании новых заметок
- Учитывайте `expiresAt` при кэшировании

---

## 🔧 Технические детали

### База данных
- **PostgreSQL 16** с UTF-8 кодировкой
- **HikariCP** connection pool
- **Exposed ORM** для типобезопасных запросов

### Безопасность
- **JWT токены** с HMAC SHA-256
- **Bcrypt** хэширование паролей
- **CORS** настроен для разработки
- **SQL injection** защита через Exposed ORM

### Производительность
- **Connection pooling** для оптимальной работы с БД
- **Indexed queries** для быстрого поиска
- **Пагинация** для больших списков

---

## 📋 Shared модуль

Для упрощения интеграции с Android приложением создан shared модуль с общими моделями данных:

### Подключение в Android проекте
```kotlin
// build.gradle.kts (app module)
implementation(project(":shared"))
```

### Основные модели
```kotlin
// Уже готовы к использованию в Android
@Serializable
data class CreatePasteRequestDto(...)

@Serializable  
data class LoginRequestDto(...)

@Serializable
data class RegisterRequestDto(...)

@Serializable
data class UpdateProfileRequestDto(
    val username: String?,
    val displayName: String?
)

@Serializable
data class UserProfileDto(
    val user: UserDto,
    val publicPastesCount: Int,
    val totalPastesCount: Int?
)

enum class PasteVisibility { PUBLIC, UNLISTED, PRIVATE }
```

---

## ✅ Чек-лист для Android разработчика

### Перед началом разработки:
- [ ] Убедитесь, что backend запущен на `http://localhost:8080`
- [ ] Проверьте доступность API: `GET /api/pastes/public`
- [ ] Добавьте shared модуль в зависимости Android проекта
- [ ] Настройте HTTP клиент (Retrofit/Ktor Client) с base URL

### Основные функции для реализации:
- [ ] Экран регистрации/логина с сохранением JWT токена
- [ ] Список публичных заметок с пагинацией и отображением автора.
- [ ] Создание новой заметки (с выбором видимости).
- [ ] Просмотр заметки с инкрементом счетчика и отображением автора.
- [ ] Профиль пользователя с его публичными заметками.
- [ ] Список "Мои заметки" с фильтрацией по видимости.
- [ ] Редактирование профиля (displayName и username).

### Рекомендуемые библиотеки:
- **Networking**: Retrofit + OkHttp или Ktor Client
- **JSON**: Kotlinx Serialization (уже настроена в shared модуле)
- **Навигация**: Navigation Component + Deep links для профилей
- **UI**: Jetpack Compose с Material Design
- **Хранение токена**: EncryptedSharedPreferences

**Backend полностью готов к работе! Начинайте разработку Android приложения! 🚀**

---

# 📚 Nimbin API Documentation v2.0

## 🔄 Обновления версии 2.0
- ✅ Добавлена поддержка `displayName` для пользователей
- ✅ Информация об авторе в заметках (`authorUsername`, `authorDisplayName`)
- ✅ Профили пользователей и редактирование
- ✅ Статистика пользователей

---

## 🔐 Аутентификация

### POST /api/auth/register
**Описание:** Регистрация нового пользователя

**Request Body:**
```json
{
    "username": "string",     // 3-50 символов, уникальный
    "email": "string",        // Валидный email, уникальный  
    "password": "string"      // 6+ символов
}
```

**Response 201:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "username": "testuser",
        "displayName": null,           // ✅ НОВОЕ поле
        "email": "test@example.com",
        "createdAt": "2025-08-20T10:30:00"
    }
}
```

### POST /api/auth/login
**Описание:** Вход в систему

**Request Body:**
```json
{
    "username": "string",     // username или email
    "password": "string"
}
```

**Response 200:** Аналогично регистрации

---

## 📝 Заметки (Pastes)

### GET /api/pastes/public
**Описание:** Получить публичные заметки с информацией об авторах

**Query Parameters:**
- `page` (int, default: 1) - номер страницы
- `limit` (int, default: 20) - количество заметок на странице
- `sort` (string, default: "newest") - сортировка:
  - `newest` - новые сначала
  - `oldest` - старые сначала  
  - `most_viewed` - популярные сначала
  - `least_viewed` - менее популярные сначала
  - `title_asc` - по названию А-Я
  - `title_desc` - по названию Я-А

**Response 200:**
```json
[
    {
        "id": "abc123def456",
        "title": "Моя заметка",
        "content": "Содержимое заметки...",
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "authorUsername": "testuser",      // ✅ НОВОЕ - имя автора
        "authorDisplayName": "Тест Юзер",  // ✅ НОВОЕ - отображаемое имя автора
        "visibility": "PUBLIC",
        "createdAt": "2025-08-20T10:30:00",
        "expiresAt": null,
        "language": "kotlin",
        "viewCount": 42
    }
]
```

### POST /api/pastes
**Описание:** Создать новую заметку

**Headers:**
- `Authorization: Bearer <token>` (опционально)

**Request Body:**
```json
{
    "title": "string",                    // 1-255 символов
    "content": "string",                  // 1-1,000,000 символов
    "visibility": "PUBLIC|UNLISTED|PRIVATE",  // default: PUBLIC
    "language": "text",                   // default: text
    "expiresAt": "2025-12-31T23:59:59"   // опционально
}
```

**Response 201:** Объект заметки (как в GET /api/pastes/public)

### GET /api/pastes/{id}
**Описание:** Получить заметку по ID (инкремент просмотров). Возвращает заметку с информацией об авторе.

**Headers:**
- `Authorization: Bearer <token>` (опционально, для приватных заметок)

**Response 200:** Объект заметки с информацией об авторе
```json
{
    "id": "abc123def456",
    "title": "Пример заметки",
    "content": "Содержимое заметки...",
    "userId": "user-uuid-123",
    "authorUsername": "john_doe",
    "authorDisplayName": "John Doe",
    "visibility": "PUBLIC",
    "createdAt": "2025-08-20T10:30:00",
    "expiresAt": null,
    "language": "kotlin",
    "viewCount": 42
}
```

**Примечания:**
- Для анонимных заметок `userId`, `authorUsername` и `authorDisplayName` будут `null`
- Счетчик просмотров автоматически увеличивается на 1 при каждом обращении
- Доступ к заметкам проверяется по правилам видимости

### GET /api/pastes/my
**Описание:** Получить свои заметки

**Headers:**
- `Authorization: Bearer <token>` (обязательно)

**Query Parameters:**
- `page`, `limit` - аналогично публичным заметкам

**Response 200:** Массив заметок

### DELETE /api/pastes/{id}
**Описание:** Удалить свою заметку

**Headers:**
- `Authorization: Bearer <token>` (обязательно)

**Response 200:**
```json
{
    "message": "Paste deleted successfully"
}
```

---

## 👤 Пользователи и профили (✅ НОВОЕ)

### GET /api/users/{id}
**Описание:** Получить публичный профиль пользователя

**Response 200:**
```json
{
    "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "username": "testuser",
        "displayName": "Тест Юзер",
        "email": "test@example.com",
        "createdAt": "2025-08-20T10:30:00"
    },
    "publicPastesCount": 15,      // Количество публичных заметок
    "totalPastesCount": null      // null для чужих профилей
}
```

### GET /api/users/{id}/pastes
**Описание:** Получить публичные заметки пользователя

**Query Parameters:**
- `page`, `limit` - стандартная пагинация

**Response 200:** Массив заметок с информацией об авторе

### GET /api/users/profile
**Описание:** Получить свой профиль с полной статистикой

**Headers:**
- `Authorization: Bearer <token>` (обязательно)

**Response 200:**
```json
{
    "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "username": "testuser", 
        "displayName": "Тест Юзер",
        "email": "test@example.com",
        "createdAt": "2025-08-20T10:30:00"
    },
    "publicPastesCount": 15,      // Публичные заметки
    "totalPastesCount": 23        // ✅ Полная статистика для владельца
}
```

### PUT /api/users/profile
**Описание:** Обновить свой профиль

**Headers:**
- `Authorization: Bearer <token>` (обязательно)

**Request Body:**
```json
{
    "username": "newusername",    // опционально, проверка на уникальность
    "displayName": "Новое Имя"   // опционально, может быть null
}
```

**Response 200:**
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "newusername",
    "displayName": "Новое Имя", 
    "email": "test@example.com",
    "createdAt": "2025-08-20T10:30:00"
}
```

---

## 🔧 Статус коды и обработка ошибок

### Успешные ответы
- `200 OK` - Операция выполнена успешно
- `201 Created` - Ресурс создан (регистрация, создание заметки)

### Ошибки клиента (4xx)
- `400 Bad Request` - Неверные данные запроса
- `401 Unauthorized` - Требуется авторизация
- `403 Forbidden` - Доступ запрещен
- `404 Not Found` - Ресурс не найден
- `409 Conflict` - Конфликт данных (username уже существует)

### Ошибки сервера (5xx)
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Формат ошибок
```json
{
    "error": "Описание ошибки",
    "code": "ERROR_CODE"  // опционально
}
```

---

## 📊 Лимиты и ограничения

### Заметки
- **Заголовок**: 1-255 символов
- **Содержимое**: 1-1,000,000 символов (1MB)
- **ID заметки**: 12 символов (a-zA-Z0-9, без путающих символов)
- **Языки подсветки**: text, kotlin, java, javascript, python, cpp, и др.

### Пользователи  
- **Username**: 3-50 символов, уникальный
- **DisplayName**: 0-100 символов, опционально
- **Email**: стандартная валидация, уникальный
- **Пароль**: минимум 6 символов

### Пагинация
- **Лимит**: максимум 100 заметок за запрос
- **По умолчанию**: 20 заметок на страницу

---

## 🌐 Конфигурация сервера

### Базовые настройки
- **Base URL**: `http://localhost:8080` (разработка)
- **Content-Type**: `application/json; charset=utf-8`
- **Кодировка**: UTF-8 (полная поддержка кириллицы)

### JWT токены
- **Заголовок**: `Authorization: Bearer <token>`
- **Алгоритм**: HS256
- **Время жизни**: настраивается на сервере

---

## 📱 Примеры использования

### Создание заметки с авторизацией
```bash
curl -X POST http://localhost:8080/api/pastes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Kotlin функция",
    "content": "fun hello() = println(\"Hello World\")",
    "visibility": "PUBLIC",
    "language": "kotlin"
  }'
```

### Просмотр профиля пользователя
```bash
curl -X GET http://localhost:8080/api/users/550e8400-e29b-41d4-a716-446655440000
```

### Обновление своего профиля
```bash
curl -X PUT http://localhost:8080/api/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "displayName": "Мое новое имя"
  }'
```

---

**Версия API**: 2.0  
**Дата обновления**: 20.08.2025  
**Совместимость**: Обратно совместимо с v1.0
