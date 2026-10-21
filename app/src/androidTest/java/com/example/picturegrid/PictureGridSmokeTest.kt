package com.example.picturegrid

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.SingletonImageLoader
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Дымовой тест приложения.
 *
 * Проверяет, что приложение поднимается на настоящем устройстве и что
 * загрузчик изображений установлен в процесс: без него Coil создаёт
 * загрузчик на каждый запрос и кэш перестаёт быть общим.
 */
@RunWith(AndroidJUnit4::class)
class PictureGridSmokeTest {

    @Test
    fun mainActivityStarts() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue("активность должна быть запущена", !activity.isFinishing)
            }
        }
    }

    @Test
    fun imageLoaderIsSharedForTheProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("загрузчик изображений должен быть установлен", SingletonImageLoader.get(context))
    }
}
