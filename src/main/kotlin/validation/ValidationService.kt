package tech.nimbus.validation

/**
 * Результат валидации.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String, val field: String? = null) : ValidationResult()
    data class Errors(val errors: List<Error>) : ValidationResult()
}

/**
 * Валидатор для входных данных.
 */
object ValidationService {

    /**
     * Валидация данных регистрации.
     */
    fun validateRegistration(username: String, email: String, password: String): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()

        // Валидация username
        validateUsername(username)?.let { errors.add(it) }

        // Валидация email
        validateEmail(email)?.let { errors.add(it) }

        // Валидация password
        validatePassword(password)?.let { errors.add(it) }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Errors(errors)
        }
    }

    /**
     * Валидация данных логина.
     */
    fun validateLogin(email: String, password: String): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()

        // Валидация email
        validateEmail(email)?.let { errors.add(it) }

        if (password.isBlank()) {
            errors.add(ValidationResult.Error("Password cannot be blank", "password"))
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Errors(errors)
        }
    }

    /**
     * Валидация создания заметки.
     */
    fun validatePasteCreation(title: String, content: String): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()

        if (title.isBlank()) {
            errors.add(ValidationResult.Error("Title cannot be blank", "title"))
        }
        if (title.length > 255) {
            errors.add(ValidationResult.Error("Title is too long (max 255 characters)", "title"))
        }

        if (content.isBlank()) {
            errors.add(ValidationResult.Error("Content cannot be blank", "content"))
        }
        if (content.length > 1_000_000) {
            errors.add(ValidationResult.Error("Content is too long (max 1MB)", "content"))
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Errors(errors)
        }
    }

    /**
     * Валидация обновления профиля.
     */
    fun validateProfileUpdate(username: String?, displayName: String?): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()

        username?.let { name ->
            validateUsername(name)?.let { errors.add(it) }
        }

        displayName?.let { name ->
            if (name.length > 100) {
                errors.add(ValidationResult.Error("Display name is too long (max 100 characters)", "displayName"))
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Errors(errors)
        }
    }

    // Приватные методы валидации

    private fun validateUsername(username: String): ValidationResult.Error? {
        return when {
            username.isBlank() -> ValidationResult.Error("Username cannot be blank", "username")
            username.length < 3 -> ValidationResult.Error("Username must be at least 3 characters long", "username")
            username.length > 50 -> ValidationResult.Error("Username is too long (max 50 characters)", "username")
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                ValidationResult.Error("Username can only contain letters, numbers, and underscores", "username")
            else -> null
        }
    }

    private fun validateEmail(email: String): ValidationResult.Error? {
        return when {
            email.isBlank() -> ValidationResult.Error("Email cannot be blank", "email")
            !isValidEmailFormat(email) -> ValidationResult.Error("Invalid email format", "email")
            email.length > 255 -> ValidationResult.Error("Email is too long (max 255 characters)", "email")
            else -> null
        }
    }

    private fun validatePassword(password: String): ValidationResult.Error? {
        return when {
            password.isBlank() -> ValidationResult.Error("Password cannot be blank", "password")
            password.length < 6 -> ValidationResult.Error("Password must be at least 6 characters long", "password")
            password.length > 255 -> ValidationResult.Error("Password is too long (max 255 characters)", "password")
            else -> null
        }
    }

    private fun isValidEmailFormat(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
        return emailRegex.matches(email)
    }
}
