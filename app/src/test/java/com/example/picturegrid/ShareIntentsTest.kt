package com.example.picturegrid

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Проверка интентов действий с URL изображения.
 */
@RunWith(RobolectricTestRunner::class)
class ShareIntentsTest {

    private val url = "https://example.com/images/1.png"

    @Test
    fun `share intent sends the url as text`() {
        val intent = ShareIntents().createShare(url)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(url, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `open in browser intent views the url`() {
        val intent = ShareIntents().createOpenInBrowser(url)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.data?.toString())
    }
}
