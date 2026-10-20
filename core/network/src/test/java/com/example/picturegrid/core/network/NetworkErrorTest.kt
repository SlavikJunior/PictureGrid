package com.example.picturegrid.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkErrorTest {

    @Test
    fun `io and timeout are retryable`() {
        assertTrue(NetworkError.Io.isRetryable())
        assertTrue(NetworkError.Timeout.isRetryable())
    }

    @Test
    fun `empty body is retryable`() {
        assertTrue(NetworkError.EmptyBody.isRetryable())
    }

    @Test
    fun `retryable http codes are retryable`() {
        assertTrue(NetworkError.Http(408).isRetryable())
        assertTrue(NetworkError.Http(429).isRetryable())
        assertTrue(NetworkError.Http(500).isRetryable())
        assertTrue(NetworkError.Http(503).isRetryable())
    }

    @Test
    fun `non retryable http codes are not retryable`() {
        assertFalse(NetworkError.Http(400).isRetryable())
        assertFalse(NetworkError.Http(401).isRetryable())
        assertFalse(NetworkError.Http(403).isRetryable())
        assertFalse(NetworkError.Http(404).isRetryable())
        assertFalse(NetworkError.Http(409).isRetryable())
    }
}
