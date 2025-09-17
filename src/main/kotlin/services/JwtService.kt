package tech.nimbus.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date

/**
 * Сервис для работы с JWT токенами.
 *
 * Обеспечивает генерацию и валидацию JSON Web Tokens для аутентификации пользователей.
 * Использует HMAC256 алгоритм для подписи токенов и включает информацию о пользователе
 * в полезную нагрузку токена.
 *
 * @property secret Секретный ключ для подписи токенов
 * @property issuer Издатель токена (обычно доменное имя приложения)
 * @property audience Аудитория токена (целевое приложение)
 */
class JwtService(
    secret: String,
    private val issuer: String,
    private val audience: String
) {
    /** Алгоритм HMAC256 для подписи токенов */
    private val algorithm = Algorithm.HMAC256(secret)

    /**
     * Генерирует JWT токен для пользователя.
     *
     * Создает подписанный JWT токен с информацией о пользователе и временем истечения.
     * Токен действителен в течение 24 часов с момента создания.
     *
     * @param userId Уникальный идентификатор пользователя
     * @return Подписанный JWT токен в виде строки
     *
     * @sample generateToken("user-uuid-here") // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    fun generateToken(userId: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("isGuest", false)
        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 3600_000))
        .sign(algorithm)

    /**
     * Генерирует гостевой JWT токен.
     *
     * Создает подписанный JWT токен с информацией для гостевого доступа.
     * Токен действителен в течение 30 дней с момента создания.
     *
     * @param guestId Уникальный идентификатор гостя
     * @param ttlMillis Время жизни токена в миллисекундах (по умолчанию 30 дней)
     * @return Подписанный гост��вой JWT токен в виде строки
     *
     * @sample generateGuestToken("guest-uuid-here") // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    fun generateGuestToken(guestId: String, ttlMillis: Long = 30L * 24 * 3600_000): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("guestId", guestId)
        .withClaim("isGuest", true)
        .withExpiresAt(Date(System.currentTimeMillis() + ttlMillis))
        .sign(algorithm)

    /**
     * Верифицирует JWT токен и возвращает его полезную нагрузку (claims).
     *
     * Проверяет подпись токена, его аудиторию и издателя.
     * Если токен действителен, возвращает объект DecodedJWT с информацией из токена.
     * Если токен недействителен, возвращает null.
     *
     * @param token JWT токен для верификации
     * @return Объект DecodedJWT с полезной нагрузкой токена или null, если токен недействителен
     *
     * @sample verify("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
     */
    fun verify(token: String): DecodedJWT? = try {
        JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
            .verify(token)
    } catch (_: Exception) {
        null
    }
}
