package tech.nimbus.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.util.*

/**
 * Unit тесты для сервиса работы с JWT токенами.
 */
class JwtServiceTest {

    private val testSecret = "test-secret-key-for-jwt-tokens-very-long-and-secure"
    private val testIssuer = "tech.nimbus.test"
    private val testAudience = "tech.nimbus.test.audience"

    private val jwtService = JwtService(
        secret = testSecret,
        issuer = testIssuer,
        audience = testAudience
    )

    @Test
    fun `generateToken должен создавать валидный JWT токен`() {
        // Given
        val userId = "test-user-123"

        // When
        val token = jwtService.generateToken(userId)

        // Then
        assertNotNull(token, "Токен не должен быть null")
        assertTrue(token.isNotEmpty(), "Токен не должен быть пустым")
        assertTrue(token.contains("."), "JWT токен должен содержать точки")
    }

    @Test
    fun `generateToken должен включать правильные claims`() {
        // Given
        val userId = "test-user-456"

        // When
        val token = jwtService.generateToken(userId)

        // Then
        val decodedJWT = JWT.require(Algorithm.HMAC256(testSecret))
            .withIssuer(testIssuer)
            .withAudience(testAudience)
            .build()
            .verify(token)

        assertEquals(testIssuer, decodedJWT.issuer, "Issuer должен совпадать")
        assertTrue(decodedJWT.audience.contains(testAudience), "Audience должен содержать тестовое значение")
        assertEquals(userId, decodedJWT.getClaim("userId").asString(), "UserId должен совпадать")
    }

    @Test
    fun `generateToken должен устанавливать время истечения`() {
        // Given
        val userId = "test-user-789"
        val beforeGeneration = Date()

        // When
        val token = jwtService.generateToken(userId)

        // Then
        val decodedJWT = JWT.require(Algorithm.HMAC256(testSecret))
            .withIssuer(testIssuer)
            .withAudience(testAudience)
            .build()
            .verify(token)

        val expiresAt = decodedJWT.expiresAt
        assertNotNull(expiresAt, "Токен должен иметь время истечения")

        // Проверяем, что время истечения примерно через 24 часа
        val expectedExpiration = Date(beforeGeneration.time + 24 * 3600_000)
        val timeDifference = Math.abs(expiresAt.time - expectedExpiration.time)
        assertTrue(timeDifference < 60_000, "Время истечения должно быть примерно через 24 часа")
    }

    @Test
    fun `generateToken должен генерировать разные токены для разных пользователей`() {
        // Given
        val userId1 = "user-1"
        val userId2 = "user-2"

        // When
        val token1 = jwtService.generateToken(userId1)
        val token2 = jwtService.generateToken(userId2)

        // Then
        assertTrue(token1 != token2, "Токены для разных пользователей должны отличаться")
    }

    @Test
    fun `generateToken должен генерировать разные токены в разное время`() {
        // Given
        val userId = "same-user"

        // When
        val token1 = jwtService.generateToken(userId)
        Thread.sleep(1000) // Ждем секунду для разного времени генерации
        val token2 = jwtService.generateToken(userId)

        // Then
        assertTrue(token1 != token2, "Токены для одного пользователя в разное время должны отличаться")
    }

    @Test
    fun `generateToken должен работать с пустым userId`() {
        // Given
        val userId = ""

        // When
        val token = jwtService.generateToken(userId)

        // Then
        val decodedJWT = JWT.require(Algorithm.HMAC256(testSecret))
            .withIssuer(testIssuer)
            .withAudience(testAudience)
            .build()
            .verify(token)

        assertEquals("", decodedJWT.getClaim("userId").asString(), "Пустой userId должен сохраняться")
    }

    @Test
    fun `generateToken должен работать с длинным userId`() {
        // Given
        val userId = "very-long-user-id-" + "x".repeat(1000)

        // When
        val token = jwtService.generateToken(userId)

        // Then
        val decodedJWT = JWT.require(Algorithm.HMAC256(testSecret))
            .withIssuer(testIssuer)
            .withAudience(testAudience)
            .build()
            .verify(token)

        assertEquals(userId, decodedJWT.getClaim("userId").asString(), "Длинный userId должен сохраняться")
    }

    @Test
    fun `generateToken должен работать с специальными символами в userId`() {
        // Given
        val userId = "user@example.com-тест-用户"

        // When
        val token = jwtService.generateToken(userId)

        // Then
        val decodedJWT = JWT.require(Algorithm.HMAC256(testSecret))
            .withIssuer(testIssuer)
            .withAudience(testAudience)
            .build()
            .verify(token)

        assertEquals(userId, decodedJWT.getClaim("userId").asString(), "UserId со спецсимволами должен сохраняться")
    }
}
