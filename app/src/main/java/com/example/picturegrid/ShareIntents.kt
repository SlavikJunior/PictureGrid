package com.example.picturegrid

import android.content.Intent
import android.net.Uri

/**
 * Интенты для действий с URL изображения.
 *
 * Вынесены отдельно, чтобы логику можно было проверить unit-тестами, а
 * Viewer-фича оставалась независимой от Android: она получает готовые
 * действия колбэками.
 */
object ShareIntents {

    /**
     * Отправка URL другим приложениям.
     */
    fun createShare(url: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }

    /**
     * Открытие URL в браузере или другом приложении, умеющем работать с
     * ссылками.
     */
    fun createOpenInBrowser(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
}
