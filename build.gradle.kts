val ktor_version: String by project
val exposed_version: String by project
val hikaricp_version: String by project
val postgresql_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("io.ktor.plugin") version "2.3.12"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    idea
}

group = "tech.nimbus"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("tech.nimbus.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

// Конфигурация для создания fat JAR
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Nimbin_back")
    archiveClassifier.set("all")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "tech.nimbus.ApplicationKt"))
    }
}

// Heroku использует stage задачу для сборки
tasks.register("stage") {
    dependsOn("shadowJar")
}

// Убираем конфликтные зависимости задач
// tasks["build"].mustRunAfter("clean")  -- убираем
// tasks["shadowJar"].mustRunAfter("build")  -- убираем

// Добавляем конфигурацию для IDEA
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories { mavenCentral() }

dependencies {
    /* ---------- Shared модуль ---------- */
    implementation(project(":shared"))

    /* ---------- Ktor ---------- */
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")

    /* Плагины Ktor */
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")   // CallLogging[2]
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")  // StatusPages[3]
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")

    /* ---------- База данных ---------- */
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")      // Exposed[14]
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")       // Exposed[14]
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")      // Exposed[9]
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version") // Exposed[14]
    implementation("com.zaxxer:HikariCP:$hikaricp_version")                    // HikariCP[10]
    implementation("org.postgresql:postgresql:$postgresql_version")            // JDBC-драйвер
    implementation("com.h2database:h2:2.1.214")                               // H2 для тестирования

    /* ---------- Логирование ---------- */
    implementation("ch.qos.logback:logback-classic:$logback_version")

    /* Добавлено для хеширования паролей */
    implementation("at.favre.lib:bcrypt:0.9.0")

    /* ---------- Тесты ---------- */
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("com.h2database:h2:2.1.214")
    testImplementation("org.testcontainers:junit-jupiter:1.18.3")
    testImplementation("org.testcontainers:postgresql:1.18.3")
    
    // Добавляем Mockito для Java-style моков (совместимо с нашими тестами)
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }
