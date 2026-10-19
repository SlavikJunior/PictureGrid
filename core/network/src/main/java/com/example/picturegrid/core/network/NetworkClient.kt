package com.example.picturegrid.core.network

interface NetworkClient {
    suspend fun fetchText(url: String): NetworkResult<String>

    /**
     * Загрузка бинарного ресурса (например, оригинала изображения).
     * Возвращает байты и Content-Type, если сервер его передал.
     * Default-реализация позволяет не ломать существующие фейки в тестах.
     */
    suspend fun fetchBytes(url: String): NetworkResult<BinaryResponse> =
        NetworkResult.Failure(NetworkError.Io)
}

/**
 * Бинарный ответ сети.
 */
data class BinaryResponse(
    val bytes: ByteArray,
    val contentType: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryResponse) return false
        return contentType == other.contentType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + (contentType?.hashCode() ?: 0)
}
