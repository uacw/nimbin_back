package tech.nimbus.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.nimbus.database.repositories.UserRepository
import tech.nimbus.routes.utils.safeExecute
import tech.nimbus.routes.utils.handleValidationResult
import tech.nimbus.services.JwtService
import tech.nimbus.shared.dto.request.RegisterRequestDto
import tech.nimbus.shared.dto.request.LoginRequestDto
import tech.nimbus.shared.dto.AuthResponseDto
import tech.nimbus.shared.dto.UserDto
import tech.nimbus.utils.DtoConverters.toUserDto
import tech.nimbus.validation.ValidationService
import tech.nimbus.exceptions.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Роуты для аутентификации с улучшенной валидацией и обработкой ошибок.
 *
 * ⚠️ ВАЖНО: Сохраняет полную совместимость с shared модулем!
 * Все DTO остаются неизменными для Android приложения.
 */
fun Route.authRoutes(jwtService: JwtService, userRepository: UserRepository) {

    route("/api/auth") {

        // Новый гостевой вход: POST /api/auth/guest
        post("/guest") {
            call.safeExecute {
                val guestId = UUID.randomUUID().toString()
                val token = jwtService.generateGuestToken(guestId)

                // Синтетический пользователь только для ответа (не сохраняется в БД)
                val nowIso = LocalDateTime.now().toString()
                val guestUser = UserDto(
                    id = guestId,
                    username = "guest-" + guestId.substring(0, 8),
                    displayName = null,
                    email = "",
                    createdAt = nowIso
                )
                call.respond(HttpStatusCode.Created, AuthResponseDto(token, guestUser))
            }
        }

        // POST /api/auth/register - регистрация
        post("/register") {
            call.safeExecute {
                val request = call.receive<RegisterRequestDto>()

                // Валидация входных данных
                val validationResult = ValidationService.validateRegistration(
                    request.username,
                    request.email,
                    request.password
                )
                if (!call.handleValidationResult(validationResult)) return@safeExecute

                // Проверяем уникальность username
                if (userRepository.findByUsername(request.username) != null) {
                    throw UserException.UsernameAlreadyExists()
                }

                // Проверяем уникальность email
                if (userRepository.findByEmail(request.email) != null) {
                    throw UserException.EmailAlreadyExists()
                }

                val userId = UUID.randomUUID().toString()

                // Создаем пользователя
                val userCreated = try {
                    userRepository.createUser(userId, request.username, request.email, request.password)
                } catch (e: Exception) {
                    throw DatabaseException.QueryFailed("createUser", e)
                }

                if (!userCreated) {
                    throw UserException.UserCreationFailed()
                }

                val user = try {
                    userRepository.getUserById(userId)
                } catch (e: Exception) {
                    throw DatabaseException.QueryFailed("getUserById", e)
                }

                if (user == null) {
                    throw UserException.UserCreationFailed()
                }

                val token = jwtService.generateToken(userId)
                call.respond(HttpStatusCode.Created, AuthResponseDto(token, user.toUserDto()))
            }
        }

        // POST /api/auth/login - авторизация
        post("/login") {
            call.safeExecute {
                val request = call.receive<LoginRequestDto>()

                // Валидация входных данных
                val validationResult = ValidationService.validateLogin(request.email, request.password)
                if (!call.handleValidationResult(validationResult)) return@safeExecute

                // Ищем пользователя по email
                val user = try {
                    userRepository.findUserByEmail(request.email)
                } catch (e: Exception) {
                    throw DatabaseException.QueryFailed("findUser", e)
                }

                if (user == null) {
                    throw AuthException.UserNotFound()
                }

                // Проверяем пароль
                val passwordValid = try {
                    userRepository.verifyPassword(request.password, user.passwordHash)
                } catch (e: Exception) {
                    throw DatabaseException.QueryFailed("verifyPassword", e)
                }

                if (!passwordValid) {
                    throw AuthException.IncorrectPassword()
                }

                val token = jwtService.generateToken(user.id)
                call.respond(HttpStatusCode.OK, AuthResponseDto(token, user.toUserDto()))
            }
        }
    }
}
