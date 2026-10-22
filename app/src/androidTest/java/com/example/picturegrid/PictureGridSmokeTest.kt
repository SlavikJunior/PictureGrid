package com.example.picturegrid

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import org.koin.core.context.GlobalContext
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
        ActivityScenario.launch(MainActivity::class.java).use {
            assertNotNull(
                "загрузчик изображений должен быть зарегистрирован в Koin",
                GlobalContext.get().get<ImageLoader>(),
            )
        }
    }
}
