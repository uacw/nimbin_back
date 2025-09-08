package tech.nimbus.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import java.net.URI

fun Application.configureDatabases() {
    // Приоритет для переменной окружения Heroku
    val databaseUrl = System.getenv("DATABASE_URL")

    val hikariConfig = if (!databaseUrl.isNullOrBlank()) {
        // Конфигурация для Heroku (производственная среда)
        val dbUri = URI(databaseUrl)
        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=require"

        HikariConfig().apply {
            jdbcUrl = dbUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
        }
    } else {
        // Конфигурация для локальной разработки - используем дефолтные значения
        HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5432/nimbin_dev"
            username = "postgres"
            password = "postgres"
        }
    }

    // Общие настройки для HikariCP
    hikariConfig.apply {
        maximumPoolSize = 10
        minimumIdle = 5
        idleTimeout = 300000
        connectionTimeout = 30000
        leakDetectionThreshold = 60000
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    // Авто-миграции и создание недостающих таблиц/колонок
    transaction {
        // Хелпер: проверка наличия колонки в таблице через INFORMATION_SCHEMA (кросс-СУБД)
        fun columnExists(table: String, column: String): Boolean {
            val sql = """
                SELECT 1
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = UPPER('$table')
                  AND UPPER(COLUMN_NAME) = UPPER('$column')
                LIMIT 1
            """.trimIndent()
            return try {
                var exists = false
                exec(sql) { rs -> if (rs.next()) exists = true }
                exists
            } catch (_: Exception) {
                false
            }
        }

        val hasLanguage = columnExists("pastes", "language")
        val hasSyntaxLang = columnExists("pastes", "syntax_language")

        // 1) Переименовать language -> syntax_language, только если старая есть и новой ещё нет
        if (hasLanguage && !hasSyntaxLang) {
            var renamed = false
            try {
                exec("ALTER TABLE pastes RENAME COLUMN language TO syntax_language")
                renamed = true
            } catch (_: Exception) {
                // Попытка синтаксиса H2
                try {
                    exec("ALTER TABLE pastes ALTER COLUMN language RENAME TO syntax_language")
                    renamed = true
                } catch (_: Exception) { /* игнор */ }
            }
            // Обновим флаги при успехе
            if (renamed) {
                // После успешного переименования отражаем новое состояние
                // чтобы последующие шаги не обращались к несуществующим колонкам
            }
        }

        val hasSyntaxNow = columnExists("pastes", "syntax_language")

        // 2) Проставить DEFAULT и заполнить NULL, только если колонка существует
        if (hasSyntaxNow) {
            try { exec("ALTER TABLE pastes ALTER COLUMN syntax_language SET DEFAULT 'plaintext'") } catch (_: Exception) {}
            try { exec("UPDATE pastes SET syntax_language='plaintext' WHERE syntax_language IS NULL") } catch (_: Exception) {}
        }

        // 3) Создать недостающие таблицы/колонки
        SchemaUtils.createMissingTablesAndColumns(UserTable, PasteTable)
    }
}
