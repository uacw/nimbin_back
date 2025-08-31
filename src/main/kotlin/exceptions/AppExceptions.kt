package tech.nimbus.exceptions

/**
 * Базовое исключение для бизнес-логики приложения.
 */
sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Исключения аутентификации.
 */
sealed class AuthException(message: String, cause: Throwable? = null) : AppException(message, cause) {
    class InvalidCredentials : AuthException("Invalid username/email or password")
    class UserNotFound : AuthException("User not found")
    class IncorrectPassword : AuthException("Incorrect password")
    class TokenExpired : AuthException("JWT token has expired")
    class InvalidToken : AuthException("Invalid JWT token")
    class AuthenticationRequired : AuthException("Authentication required")
}

/**
 * Исключения пользователей.
 */
sealed class UserException(message: String, cause: Throwable? = null) : AppException(message, cause) {
    class UserAlreadyExists(field: String) : UserException("$field already exists")
    class UsernameAlreadyExists : UserException("Username already exists")
    class EmailAlreadyExists : UserException("Email already exists")
    class UserCreationFailed : UserException("Failed to create user")
    class UserNotFound(userId: String) : UserException("User not found: $userId")
    class ProfileUpdateFailed : UserException("Failed to update user profile")
}

/**
 * Исключения заметок.
 */
sealed class PasteException(message: String, cause: Throwable? = null) : AppException(message, cause) {
    class PasteNotFound(pasteId: String) : PasteException("Paste not found: $pasteId")
    class AccessDenied(pasteId: String) : PasteException("Access denied to paste: $pasteId")
    class PasteCreationFailed : PasteException("Failed to create paste")
    class PasteDeletionFailed : PasteException("Failed to delete paste")
    class InvalidPasteId(pasteId: String) : PasteException("Invalid paste ID format: $pasteId")
    class PrivatePasteRequiresAuth : PasteException("Authentication required for private pastes")
}

/**
 * Исключения валидации.
 */
sealed class ValidationException(message: String, val field: String? = null, cause: Throwable? = null) :
    AppException(message, cause) {
    class InvalidInput(message: String, field: String? = null) : ValidationException(message, field)
    class RequiredField(field: String) : ValidationException("$field is required", field)
    class InvalidFormat(field: String, format: String) : ValidationException("Invalid $field format: $format", field)
    class ValueTooLong(field: String, maxLength: Int) : ValidationException("$field is too long (max $maxLength characters)", field)
    class ValueTooShort(field: String, minLength: Int) : ValidationException("$field is too short (min $minLength characters)", field)
}

/**
 * Исключения базы данных.
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) : AppException(message, cause) {
    class ConnectionFailed : DatabaseException("Database connection failed")
    class QueryFailed(query: String, cause: Throwable) : DatabaseException("Database query failed: $query", cause)
    class TransactionFailed : DatabaseException("Database transaction failed")
    class ConstraintViolation(constraint: String) : DatabaseException("Database constraint violation: $constraint")
    class DataIntegrityError : DatabaseException("Data integrity error")
}

/**
 * Исключения пагинации.
 */
sealed class PaginationException(message: String, cause: Throwable? = null) : AppException(message, cause) {
    class InvalidPageNumber(page: Int) : PaginationException("Invalid page number: $page")
    class InvalidLimit(limit: Int) : PaginationException("Invalid limit: $limit (must be between 1 and 100)")
    class InvalidOffset(offset: Int) : PaginationException("Invalid offset: $offset (must be non-negative)")
}
