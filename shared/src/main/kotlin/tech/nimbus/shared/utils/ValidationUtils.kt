package tech.nimbus.shared.utils

/**
 * Утилиты для валидации данных в shared модуле.
 */
object ValidationUtils {
    
    // Paste validation
    const val MIN_TITLE_LENGTH = 1
    const val MAX_TITLE_LENGTH = 255
    const val MIN_CONTENT_LENGTH = 1
    const val MAX_CONTENT_LENGTH = 1_000_000 // 1MB
    const val PASTE_ID_LENGTH = 12
    
    // User validation
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 50
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PASSWORD_LENGTH = 128
    
    // Supported languages for syntax highlighting
    val SUPPORTED_LANGUAGES = setOf(
        "text", "kotlin", "java", "javascript", "typescript", "python", 
        "cpp", "c", "csharp", "php", "ruby", "go", "rust", "swift",
        "html", "css", "xml", "json", "yaml", "sql", "bash", "powershell"
    )
    
    /**
     * Проверить валидность заголовка заметки
     */
    fun isValidTitle(title: String): Boolean {
        return title.length in MIN_TITLE_LENGTH..MAX_TITLE_LENGTH
    }
    
    /**
     * Проверить валидность содержимого заметки
     */
    fun isValidContent(content: String): Boolean {
        return content.length in MIN_CONTENT_LENGTH..MAX_CONTENT_LENGTH
    }
    
    /**
     * Проверить валидность ID заметки
     */
    fun isValidPasteId(id: String): Boolean {
        return id.length == PASTE_ID_LENGTH && id.all { it.isLetterOrDigit() }
    }
    
    /**
     * Проверить валидность имени пользователя
     */
    fun isValidUsername(username: String): Boolean {
        return username.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH &&
                username.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }
    
    /**
     * Проверить валидность email
     */
    fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length <= 255
    }
    
    /**
     * Проверить валидность пароля
     */
    fun isValidPassword(password: String): Boolean {
        return password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
    }
    
    /**
     * Проверить поддерживается ли язык программирования
     */
    fun isValidLanguage(language: String): Boolean {
        return language.lowercase() in SUPPORTED_LANGUAGES
    }
    
    /**
     * Нормализовать язык программирования
     */
    fun normalizeLanguage(language: String): String {
        val normalized = language.lowercase().trim()
        return if (isValidLanguage(normalized)) normalized else "text"
    }
}
