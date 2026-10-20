package com.example.picturegrid.theme

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.picturegrid.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверка того, что для ночного режима определена отдельная тема окна.
 *
 * Тема android:Theme.Material.Light принудительно сбрасывает uiMode в
 * notnight: приложение всегда получало светлую палитру Compose, а
 * `isSystemInDarkTheme()` возвращал false. Поэтому ночная тема окна
 * должна задавать собственный (тёмный) фон окна.
 */
@RunWith(RobolectricTestRunner::class)
class DarkThemeResourceTest {

    @Test
    @Config(qualifiers = "night")
    fun `night theme defines a dark window background`() {
        val background = windowBackgroundColor()

        assertTrue("Фон окна в ночной теме должен быть тёмным: $background", isDark(background))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `day theme defines a light window background`() {
        val background = windowBackgroundColor()

        assertTrue("Фон окна в дневной теме должен быть светлым: $background", !isDark(background))
    }

    private fun windowBackgroundColor(): Int {
        val context: Context = ApplicationProvider.getApplicationContext()
        val theme = ContextThemeWrapper(context, R.style.Theme_PictureGrid).theme
        val typedValue = TypedValue()
        val resolved = theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        require(resolved) { "Атрибут windowBackground не найден в теме" }
        return context.resources.getColor(typedValue.resourceId, theme)
    }

    private fun isDark(color: Int): Boolean =
        Color.red(color) + Color.green(color) + Color.blue(color) < 3 * 128
}
