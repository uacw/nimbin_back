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

    val hikariConfig = if (databaseUrl != null) {
        // Конфигурация для Heroku
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
        // Конфигурация для локальной разработки из application.yml
        HikariConfig().apply {
            driverClassName = environment.config.property("database.driver").getString()
            jdbcUrl = environment.config.property("database.url").getString()
            username = environment.config.property("database.user").getString()
            password = environment.config.property("database.password").getString()
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
