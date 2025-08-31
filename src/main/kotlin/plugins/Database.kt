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

    // Автоматически создаём/обновляем таблицы при старте
    transaction {
        SchemaUtils.create(UserTable, PasteTable)
    }
}
