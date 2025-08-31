package tech.nimbus.shared.utils

/**
 * Sealed class для обработки результатов API операций.
 * Позволяет безопасно обрабатывать успешные результаты и ошибки.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    
    /**
     * Проверить является ли результат успешным
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Проверить является ли результат ошибкой
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Получить данные или null в случае ошибки
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Получить данные или значение по умолчанию
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }
    
    /**
     * Выполнить действие если результат успешный
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Выполнить действие если результат содержит ошибку
     */
    inline fun onError(action: (String, Int?) -> Unit): ApiResult<T> {
        if (this is Error) action(message, code)
        return this
    }
    
    /**
     * Преобразовать результат в другой тип
     */
    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, code)
    }
}
