# Nimbin Backend - Ktor REST API для Pastebin-клона

[![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)](build/reports/tests/test/index.html)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-purple.svg)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/ktor-2.3.12-blue.svg)](https://ktor.io/)
[![PostgreSQL](https://img.shields.io/badge/postgresql-16-blue.svg)](https://postgresql.org/)
[![Heroku](https://img.shields.io/badge/deployed-heroku-430098.svg)](https://nimbin-back-1de949af6629.herokuapp.com)

Современный и надежный REST API для Android приложения типа **Pastebin/Ghostbin**, построенный на Kotlin/Ktor. Предоставляет полный функционал для создания, хранения и обмена текстовыми заметками с продвинутой системой управления приватностью и пользовательскими профилями.

## 🌟 Особенности проекта

### 🔐 **Безопасность и аутентификация**
- JWT токены с 24-часовым сроком действия
- Bcrypt хеширование паролей (cost factor 12)
- Защищенные endpoints с проверкой прав доступа
- CORS настройка для кроссдоменных запросов

### 👤 **Пользовательские профили**
- Регистрация и аутентификация по email
- Персональные профили с username и displayName
- Публичные профили с количеством заметок
- Возможность редактирования профиля

### 📝 **Система заметок с уровнями приватности**
- **PUBLIC** — публичные заметки, видимые всем
- **UNLISTED** — доступны только по прямой ссылке
- **PRIVATE** — личные заметки, видимые только владельцу
- Поддержка синтаксической подсветки для разных языков
- Автоматический счетчик просмотров
- Опциональное время истечения заметок

### ⭐ **Избранное (MVP)**
- Добавление/удаление заметок в «Избранное»
- Флаг `isFavorite` в ответах (при запросе с токеном)
- Фильтр моих заметок по избранному (`?favorite=true`)

### 🔍 **Расширенные возможности**
- Пагинация для всех списков
- Сортировка по дате, названию или популярности
- Фильтрация заметок по уровню приватности
- Полная поддержка UTF-8 и кириллицы
- Уникальные ID без трудноразличимых символов

## 🚀 Технологический стек

- **Backend Framework**: Kotlin + Ktor Server 2.3.12
- **Database**: PostgreSQL с HikariCP connection pool
- **ORM**: Exposed для типобезопасных SQL запросов
- **Authentication**: JWT токены с HMAC-SHA256
- **Build System**: Gradle с Kotlin DSL
- **Deployment**: Heroku с PostgreSQL addon
- **Testing**: JUnit с высоким покрытием кода (87%)

## ✅ Статус проекта

🎉 **ГОТОВ К PRODUCTION И ИНТЕГРАЦИИ**

- ✅ **Все основные функции реализованы и протестированы**
- ✅ **JWT аутентификация работает стабильно**
- ✅ **База данных PostgreSQL настроена и оптимизирована**
- ✅ **Comprehensive тестирование (87% покрытие кода)**
- ✅ **Развернуто на Heroku и доступно 24/7**
- ✅ **Shared модуль для легкой интеграции с Android**
- ✅ **Полная документация API готова**

## 🌐 Демо и доступ

**Production API**: `https://nimbin-back-1de949af6629.herokuapp.com`

Попробуйте API прямо сейчас:
- **Публичные заметки**: `GET /api/pastes/public`
- **Создание заметки**: `POST /api/pastes`
- **Регистрация**: `POST /api/auth/register`

## 📋 Основные API Endpoints

### Аутентификация
- `POST /api/auth/register` — Регистрация нового пользователя
- `POST /api/auth/login` — Вход по email и паролю

### Пользовательские профили
- `GET /api/users/{userId}` — Публичный профиль пользователя
- `GET /api/users/profile` — Получить свой профиль
- `PUT /api/users/profile` — Обновить профиль

### Управление заметками
- `POST /api/pastes` — Создать новую заметку
- `GET /api/pastes/{id}` — Получить заметку по ID
- `GET /api/pastes/public` — Список публичных заметок (при наличии токена элементы содержат `isFavorite`)
- `GET /api/pastes/my` — Мои заметки с фильтрацией (поддерживает `?favorite=true`)
- `POST /api/pastes/{id}/favorite` — Добавить заметку в избранное
- `DELETE /api/pastes/{id}/favorite` — Удалить заметку из избранного
- `DELETE /api/pastes/{id}` — Удалить заметку

### Дополнительные возможности
- Пагинация: `?limit=20&offset=0`
- Сортировка: `?sort=createdAt&order=desc`
- Фильтрация: `?visibility=PUBLIC`

## 🏗️ Архитектура

Проект следует принципам **Clean Architecture** с четким разделением слоев:

- **API Layer** — REST endpoints с валидацией
- **Service Layer** — Бизнес-логика и JWT сервисы
- **Repository Layer** — Доступ к данным через Exposed ORM  
- **Database Layer** — PostgreSQL с оптимизированными запросами

## 📱 Интеграция с Android

### Shared модуль
Для упрощения разработки Android приложения создан **shared модуль** с готовыми:
- DTO моделями для всех API запросов (включая `UpdatePasteRequestDto`)
- Enum'ами и константами
- Сериализацией через Kotlinx Serialization

## 🧪 Качество кода

- **87% покрытие тестами** — Unit тесты для всех компонентов
- **Kotlin Code Style** — соответствие стандартам Kotlin
- **Type Safety** — использование Exposed ORM для типобезопасности
- **Error Handling** — централизованная обработка ошибок
- **Documentation** — подробная документация API

## 📖 Документация

- **[API Documentation](API_DOCUMENTATION.md)** — Полная документация endpoints (включая ETag и Избранное)
- **[Shared Module Guide](shared/SHARED_MODULE_GUIDE.md)** — Руководство по интеграции shared модуля
- **[Postman Collection](Nimbin_Backend_Postman_Collection.json)** — Готовая коллекция для тестирования

## 🛠️ Для разработчиков

Смотри API_DOCUMENTATION.md для деталей запуска и ETag.

## 🎯 Использование

Этот backend подходит для:
- Android приложений типа Pastebin/Ghostbin
- Обмена кодом и текстовыми заметками
- Изучения архитектуры Kotlin/Ktor
- Стажировочных проектов с production деплоем

## 📞 Контакты

**Production API**: `https://nimbin-back-1de949af6629.herokuapp.com`

Проект создан для демонстрации навыков backend разработки на Kotlin/Ktor со современными практиками и готовностью к production.
