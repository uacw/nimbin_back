package tech.nimbus.exceptions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Unit тесты для системы исключений приложения.
 * Проверяет корректность создания и наследования исключений.
 */
@DisplayName("Application Exceptions Tests")
class AppExceptionsTest {

    @Nested
    @DisplayName("Auth Exceptions")
    inner class AuthExceptionsTest {

        @Test
        @DisplayName("Should create InvalidCredentials exception")
        fun shouldCreateInvalidCredentialsException() {
            val exception = AuthException.InvalidCredentials()

            assertEquals("Invalid username/email or password", exception.message)
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create UserNotFound exception")
        fun shouldCreateUserNotFoundException() {
            val exception = AuthException.UserNotFound()

            assertEquals("User not found", exception.message)
        }

        @Test
        @DisplayName("Should create IncorrectPassword exception")
        fun shouldCreateIncorrectPasswordException() {
            val exception = AuthException.IncorrectPassword()

            assertEquals("Incorrect password", exception.message)
        }

        @Test
        @DisplayName("Should create TokenExpired exception")
        fun shouldCreateTokenExpiredException() {
            val exception = AuthException.TokenExpired()

            assertEquals("JWT token has expired", exception.message)
        }

        @Test
        @DisplayName("Should create AuthenticationRequired exception")
        fun shouldCreateAuthenticationRequiredException() {
            val exception = AuthException.AuthenticationRequired()

            assertEquals("Authentication required", exception.message)
        }
    }

    @Nested
    @DisplayName("User Exceptions")
    inner class UserExceptionsTest {

        @Test
        @DisplayName("Should create UserAlreadyExists exception with field")
        fun shouldCreateUserAlreadyExistsExceptionWithField() {
            val exception = UserException.UserAlreadyExists("username")

            assertEquals("username already exists", exception.message)
        }

        @Test
        @DisplayName("Should create UsernameAlreadyExists exception")
        fun shouldCreateUsernameAlreadyExistsException() {
            val exception = UserException.UsernameAlreadyExists()

            assertEquals("Username already exists", exception.message)
        }

        @Test
        @DisplayName("Should create EmailAlreadyExists exception")
        fun shouldCreateEmailAlreadyExistsException() {
            val exception = UserException.EmailAlreadyExists()

            assertEquals("Email already exists", exception.message)
        }

        @Test
        @DisplayName("Should create UserNotFound exception with userId")
        fun shouldCreateUserNotFoundExceptionWithUserId() {
            val exception = UserException.UserNotFound("user123")

            assertEquals("User not found: user123", exception.message)
        }
    }

    @Nested
    @DisplayName("Paste Exceptions")
    inner class PasteExceptionsTest {

        @Test
        @DisplayName("Should create PasteNotFound exception with pasteId")
        fun shouldCreatePasteNotFoundExceptionWithPasteId() {
            val exception = PasteException.PasteNotFound("paste123")

            assertEquals("Paste not found: paste123", exception.message)
        }

        @Test
        @DisplayName("Should create AccessDenied exception with pasteId")
        fun shouldCreateAccessDeniedExceptionWithPasteId() {
            val exception = PasteException.AccessDenied("paste123")

            assertEquals("Access denied to paste: paste123", exception.message)
        }

        @Test
        @DisplayName("Should create InvalidPasteId exception with pasteId")
        fun shouldCreateInvalidPasteIdExceptionWithPasteId() {
            val exception = PasteException.InvalidPasteId("invalid")

            assertEquals("Invalid paste ID format: invalid", exception.message)
        }

        @Test
        @DisplayName("Should create PrivatePasteRequiresAuth exception")
        fun shouldCreatePrivatePasteRequiresAuthException() {
            val exception = PasteException.PrivatePasteRequiresAuth()

            assertEquals("Authentication required for private pastes", exception.message)
        }
    }

    @Nested
    @DisplayName("Validation Exceptions")
    inner class ValidationExceptionsTest {

        @Test
        @DisplayName("Should create InvalidInput exception with field")
        fun shouldCreateInvalidInputExceptionWithField() {
            val exception = ValidationException.InvalidInput("Invalid email format", "email")

            assertEquals("Invalid email format", exception.message)
            assertEquals("email", exception.field)
        }

        @Test
        @DisplayName("Should create RequiredField exception")
        fun shouldCreateRequiredFieldException() {
            val exception = ValidationException.RequiredField("username")

            assertEquals("username is required", exception.message)
            assertEquals("username", exception.field)
        }

        @Test
        @DisplayName("Should create ValueTooLong exception")
        fun shouldCreateValueTooLongException() {
            val exception = ValidationException.ValueTooLong("title", 255)

            assertEquals("title is too long (max 255 characters)", exception.message)
            assertEquals("title", exception.field)
        }

        @Test
        @DisplayName("Should create ValueTooShort exception")
        fun shouldCreateValueTooShortException() {
            val exception = ValidationException.ValueTooShort("password", 6)

            assertEquals("password is too short (min 6 characters)", exception.message)
            assertEquals("password", exception.field)
        }
    }

    @Nested
    @DisplayName("Database Exceptions")
    inner class DatabaseExceptionsTest {

        @Test
        @DisplayName("Should create ConnectionFailed exception")
        fun shouldCreateConnectionFailedException() {
            val exception = DatabaseException.ConnectionFailed()

            assertEquals("Database connection failed", exception.message)
        }

        @Test
        @DisplayName("Should create QueryFailed exception with cause")
        fun shouldCreateQueryFailedExceptionWithCause() {
            val cause = RuntimeException("Connection timeout")
            val exception = DatabaseException.QueryFailed("SELECT * FROM pastes", cause)

            assertEquals("Database query failed: SELECT * FROM pastes", exception.message)
            assertEquals(cause, exception.cause)
        }

        @Test
        @DisplayName("Should create ConstraintViolation exception")
        fun shouldCreateConstraintViolationException() {
            val exception = DatabaseException.ConstraintViolation("unique_username")

            assertEquals("Database constraint violation: unique_username", exception.message)
        }
    }

    @Nested
    @DisplayName("Pagination Exceptions")
    inner class PaginationExceptionsTest {

        @Test
        @DisplayName("Should create InvalidLimit exception")
        fun shouldCreateInvalidLimitException() {
            val exception = PaginationException.InvalidLimit(150)

            assertEquals("Invalid limit: 150 (must be between 1 and 100)", exception.message)
        }

        @Test
        @DisplayName("Should create InvalidOffset exception")
        fun shouldCreateInvalidOffsetException() {
            val exception = PaginationException.InvalidOffset(-5)

            assertEquals("Invalid offset: -5 (must be non-negative)", exception.message)
        }
    }

    @Nested
    @DisplayName("Exception Inheritance")
    inner class ExceptionInheritanceTest {

        @Test
        @DisplayName("All exceptions should inherit from AppException")
        fun allExceptionsShouldInheritFromAppException() {
            assertTrue(AuthException.InvalidCredentials() is AppException)
            assertTrue(UserException.UserNotFound("test") is AppException)
            assertTrue(PasteException.PasteNotFound("test") is AppException)
            assertTrue(ValidationException.InvalidInput("test") is AppException)
            assertTrue(DatabaseException.ConnectionFailed() is AppException)
            assertTrue(PaginationException.InvalidLimit(0) is AppException)
        }

        @Test
        @DisplayName("All exceptions should inherit from Exception")
        fun allExceptionsShouldInheritFromException() {
            assertTrue(AuthException.InvalidCredentials() is Exception)
            assertTrue(UserException.UserNotFound("test") is Exception)
            assertTrue(PasteException.PasteNotFound("test") is Exception)
            assertTrue(ValidationException.InvalidInput("test") is Exception)
            assertTrue(DatabaseException.ConnectionFailed() is Exception)
            assertTrue(PaginationException.InvalidLimit(0) is Exception)
        }
    }
}
