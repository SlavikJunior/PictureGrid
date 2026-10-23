package com.example.picturegrid.core.network

sealed interface NetworkError {
    data object Io : NetworkError
    data object Timeout : NetworkError
    data class Http(val code: Int) : NetworkError
    data object EmptyBody : NetworkError

    /**
     * Тело ответа превысило допустимый объём [NetworkLimits].
     *
     * Ошибка не является временной: повторная попытка вернёт тот же
     * oversized-ответ, поэтому автоматический retry не выполняется.
     */
    data class TooLarge(val limitBytes: Long) : NetworkError
}

/**
 * Признак временной ошибки, для которой допустима повторная попытка.
 */
fun NetworkError.isRetryable(): Boolean = when (this) {
    NetworkError.Io, NetworkError.Timeout, NetworkError.EmptyBody -> true
    is NetworkError.Http -> code == 408 || code == 429 || code in 500..599
    is NetworkError.TooLarge -> false
}
