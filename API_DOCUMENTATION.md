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

Строго: логин по email, не по username.

---

## Контроль версий через ETag

- Для одиночного получения заметки сервер возвращает заголовок ответа `ETag: "<hash>"`.
- Значение ETag формируется из содержимого заметки и времени её обновления.
- Для обновления заметки необходимо передать заголовок запроса `If-Match: "<текущий-etag>"`.
- Если заголовок отсутствует — `428 Precondition Required`.
- Если ETag не совпадает (заметка была изменена) — `412 Precondition Failed`.
- В успешных ответах на создание/обновление сервер возвращает новый ETag в заголовке и (если доступно) в поле `etag` тела ответа.

---

## 🔐 Аутентификация и авторизация

### Поток аутентификации для Android приложения:

1. Регистрация нового пользователя: `POST /api/auth/register`
2. Логин существующего пользователя: `POST /api/auth/login` 
3. Сохранение JWT токена в SharedPreferences или защищенном хранилище
4. Передача токена во всех защищенных запросах через заголовок `Authorization: Bearer <token>`
5. Обновление токена при истечении (через повторный логин)

### Время жизни токена:
- 24 часа с момента выпуска
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
- 400 Bad Request — некорректные данные валидации
- 409 Conflict — пользователь с таким username или email уже существует

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

Важное изменение: логин строго по email адресу (не по username).

**Возможные ошибки:**
- 400 Bad Request — некорректные данные
- 401 Unauthorized — неверный email или пароль

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

Примечание: `totalPastesCount` всегда `null` для чужих профилей по соображениям приватности.

#### Получить публичные заметки пользователя
```http
GET /api/users/{userId}/pastes?sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
```

Параметры:
- `sort`: createdAt (по умолчанию), title, viewCount
- `order`: desc (по умолчанию), asc
- `limit`: максимум заметок (по умолчанию 20, максимум 100)
- `offset`: пропустить заметок (для пагинации)

**Ответ (200 OK):** массив объектов заметок с информацией об авторе.

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

Примечание: `totalPastesCount` включает все заметки (включая PRIVATE) только для владельца профиля.

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

Возможные ошибки: 400, 401, 409.

---

### 📄 Управление заметками

#### Модель ответа заметки (PasteDto)
Полезные поля:
- `updatedAt` — время последнего обновления
- `etag` — текущая версия для If-Match
- `syntaxLanguage` — язык подсветки
- `isFavorite` — признак, что заметка в избранном у текущего пользователя (присутствует при запросах с токеном)

#### Создать заметку
```http
POST /api/pastes
Content-Type: application/json
Authorization: Bearer <jwt_token>  # Опционально для PUBLIC/UNLISTED, обязательно для PRIVATE

{
    "title": "Название заметки",
    "content": "Содержимое заметки",
    "syntaxLanguage": "kotlin",
    "visibility": "PUBLIC",
    "expiresAt": "2025-12-31T23:59:59"
}
```

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
    "updatedAt": "2025-08-25T10:30:00",
    "expiresAt": null,
    "syntaxLanguage": "kotlin",
    "viewCount": 0,
    "etag": "b3d6c0...",
    "isFavorite": false
}
```

В заголовках ответа присутствует `ETag: "b3d6c0..."`.

#### Получить заметку по ID
```http
GET /api/pastes/{pasteId}
Authorization: Bearer <jwt_token>  # Для приватных заметок; при наличии вернётся `isFavorite`
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
    "updatedAt": "2025-08-25T10:40:00",
    "expiresAt": null,
    "syntaxLanguage": "kotlin",
    "viewCount": 42,
    "etag": "b3d6c0...",
    "isFavorite": true
}
```

Особенности:
- В заголовке ответа присутствует `ETag`.
- При наличии токена поле `isFavorite` отражает статус в избранном для текущего пользователя.

#### Обновить заметку (If-Match / ETag)
```http
PUT /api/pastes/{pasteId}
Authorization: Bearer <jwt_token>
If-Match: "<текущий-etag>"
Content-Type: application/json

{
  "title": "Новое название",           // опционально
  "content": "Новое содержимое",       // опционально
  "syntaxLanguage": "kotlin",          // опционально
  "visibility": "PUBLIC",              // опционально (PUBLIC|UNLISTED|PRIVATE)
  "expiresAt": "2025-12-31T23:59:59"   // опционально
}
```

**Ответ (200 OK):** — аналогично `GET /api/pastes/{pasteId}` с новым `etag`.

**Возможные ошибки:** 401, 403, 404, 428, 412.

#### Получить публичные заметки
```http
GET /api/pastes/public?sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
Authorization: Bearer <jwt_token>  # Необязательно; при наличии в элементах будет `isFavorite`
```

**Ответ (200 OK):** массив `PasteDto` (при наличии токена в каждом элементе может быть `isFavorite`).

#### Получить свои заметки (требует аутентификации)
```http
GET /api/pastes/my?visibility={ALL|PUBLIC|UNLISTED|PRIVATE}&sort={createdAt|title|viewCount}&order={asc|desc}&limit=20&offset=0
Authorization: Bearer <jwt_token>
```

Параметры:
- `visibility`: ALL (по умолчанию), PUBLIC, UNLISTED, PRIVATE
- `sort`: createdAt (по умолчанию), title, viewCount
- `order`: desc (по умолчанию), asc
- `limit`: максимум 100 (по умолчанию 20)
- `offset`: смещение
- `favorite`: `true` — вернуть только избранные заметки (новый параметр)

**Пример:** `GET /api/pastes/my?favorite=true`

#### Удалить заметку (требует аутентификации)
```http
DELETE /api/pastes/{pasteId}
Authorization: Bearer <jwt_token>
```

**Ответ (200 OK):** `{ "message": "Paste deleted successfully" }`

---

### ⭐ Избранное (MVP)

#### Добавить заметку в избранное
```http
POST /api/pastes/{pasteId}/favorite
Authorization: Bearer <jwt_token>
```

**Ответ (200 OK):** `{ "message": "Added to favorites" }`

**Ошибки:** 401, 404 (если нет доступа к заметке)

#### Удалить заметку из избранного
```http
DELETE /api/pastes/{pasteId}/favorite
Authorization: Bearer <jwt_token>
```

**Ответ (200 OK):** `{ "message": "Removed from favorites" }`

**Ошибки:** 401

---

## 🎯 Рекомендации для Android разработчиков

### Структура данных для UI
В ответах API заметки содержат информацию об авторе и флаги:
```json
{
  "userId": "uuid-string",
  "authorUsername": "john_doe",
  "authorDisplayName": "John Doe",
  "updatedAt": "ISO",
  "etag": "строка",
  "isFavorite": true
}
```

Рекомендации:
- Показывайте `authorDisplayName` как основное имя. Если `null`, используйте `authorUsername`.
- Для оптимистичного обновления заметок сохраняйте полученный `etag` и передавайте его в `If-Match` при PUT.

### Обработка ошибок
API возвращает ошибки в формате:
```json
{ "error": "Описание ошибки" }
```

Коды: 200, 201, 400, 401, 403, 404, 409, 412, 428, 500.

### Пагинация
Списочные endpoints поддерживают `limit` и `offset`.

---

## 🔧 Технические детали

- PostgreSQL 16, HikariCP, Exposed ORM
- JWT (HS256), Bcrypt, CORS
- Индексация ключевых полей, пагинация, кэширование на клиенте с ETag

---

## 📋 Shared модуль для Android интеграции

Для упрощения интеграции создан shared модуль с общими моделями данных (Kotlinx Serialization). Актуальные поля `PasteDto`: `createdAt`, `updatedAt`, `etag`, `syntaxLanguage`, `isFavorite`.

---

*Документация актуализирована: 13 сентября 2025*
