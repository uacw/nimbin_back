package tech.nimbus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tech.nimbus.plugins.*

/**
 * Nimbin Backend - Ktor сервер для Android Pastebin-клона
 *
 * Основная точка входа в приложение. Настраивает и запускает HTTP сервер
 * с поддержкой REST API для создания, хранения и обмена текстовыми заметками.
 *
 * Технологический стек:
 * - Ktor Server 2.3.7 (HTTP сервер)
 * - PostgreSQL + Exposed ORM (база данных)
 * - JWT аутентификация
 * - JSON сериализация
 *
 * @author Nimbus Tech
 * @version 1.0.0
 */

/**
 * Точка входа в приложение.
 *
 * Создает и запускает встроенный Netty сервер.
 * Порт определяется переменной окружения PORT (для Heroku) или по умолчанию 8080.
 * Сервер принимает подключения на всех сетевых интерфейсах (0.0.0.0).
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Главный модуль приложения.
 *
 * Конфигурирует все плагины и компоненты Ktor в правильном порядке:
 * 1. Database - подключение к PostgreSQL
 * 2. Serialization - JSON сериализация
 * 3. HTTP - CORS, логирование, обработка ошибок
 * 4. Security - JWT аутентификация
 * 5. Routing - API роуты
 *
 * @receiver Application контекст Ktor приложения
 */
fun Application.module() {
    configureDatabases()
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
}
