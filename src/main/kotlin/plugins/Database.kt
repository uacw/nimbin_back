package tech.nimbus.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tech.nimbus.database.tables.PasteTable
import tech.nimbus.database.tables.UserTable
import tech.nimbus.database.tables.UserFavoritesTable
import java.net.URI

fun Application.configureDatabases() {
    // Приоритет для переменной окружения Heroku
    val databaseUrl = System.getenv("DATABASE_URL")

    val hikariConfig = if (!databaseUrl.isNullOrBlank()) {
        // Конфигурация для Heroku (производс��венная среда)
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

        // 2.1) Нормализация значений visibility: только PUBLIC/UNLISTED/PRIVATE
        try {
            exec("UPDATE pastes SET visibility = UPPER(visibility) WHERE visibility IS NOT NULL")
            exec("UPDATE pastes SET visibility = 'PUBLIC' WHERE visibility IS NULL OR visibility NOT IN ('PUBLIC','UNLISTED','PRIVATE')")
        } catch (_: Exception) { /* best-effort */ }

        // 3) Создать недостающие таблицы/колонки
        // NEW: ensure updated_at exists and is populated
        val hasUpdatedAt = columnExists("pastes", "updated_at")
        if (!hasUpdatedAt) {
            try { exec("ALTER TABLE pastes ADD COLUMN updated_at TIMESTAMP NULL") } catch (_: Exception) {}
            // backfill from created_at if possible, otherwise current timestamp
            try { exec("UPDATE pastes SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL") } catch (_: Exception) {}
            try { exec("ALTER TABLE pastes ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP") } catch (_: Exception) {}
        }

        // NEW: favorites table (idempotent)
        try {
            exec(
                """
                CREATE TABLE IF NOT EXISTS user_favorites (
                    user_id  VARCHAR(36) NOT NULL,
                    paste_id VARCHAR(12) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT pk_user_favorites PRIMARY KEY (user_id, paste_id),
                    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id)
                        REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT fk_user_favorites_paste FOREIGN KEY (paste_id)
                        REFERENCES pastes(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS idx_user_favorites_user  ON user_favorites(user_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_user_favorites_paste ON user_favorites(paste_id)")
        } catch (_: Exception) { /* idempotent */ }

        // === Guest mode groundwork: add guest_id columns and indexes (idempotent) ===
        // 1) pastes.guest_id
        try { exec("ALTER TABLE pastes ADD COLUMN IF NOT EXISTS guest_id VARCHAR(36)") } catch (_: Exception) {}
        try { exec("CREATE INDEX IF NOT EXISTS idx_pastes_guest_id ON pastes(guest_id)") } catch (_: Exception) {}

        // 2) user_favorites.guest_id
        try { exec("ALTER TABLE user_favorites ADD COLUMN IF NOT EXISTS guest_id VARCHAR(36)") } catch (_: Exception) {}
        try { exec("CREATE INDEX IF NOT EXISTS idx_user_favorites_guest_id ON user_favorites(guest_id)") } catch (_: Exception) {}

        // 3) Partial unique indexes to avoid duplicates per user/guest (PostgreSQL only)
        try { exec("CREATE UNIQUE INDEX IF NOT EXISTS ux_user_favorites_guest ON user_favorites(guest_id, paste_id) WHERE guest_id IS NOT NULL") } catch (_: Exception) {}
        // Для user_id уникальность уже обеспечена PK (user_id, paste_id)

        // Создание недостающих таблиц/колонок по декларациям Exposed
        SchemaUtils.createMissingTablesAndColumns(UserTable, PasteTable, UserFavoritesTable)
    }
}