package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-22 — constructor injection, без service locator в `domain` и `data`.
 *
 * Зависимости попадают в класс через конструктор. Единственные места, где
 * разрешено доставать их из контейнера, — точка входа (`Activity`,
 * `Application`) и Route-слой (`koinViewModel()`, `koinInject()` в
 * `*Screen.kt`). Если `domain` или `data` вызывают `getKoin()`/`GlobalContext`,
 * граф зависимостей перестаёт быть видимым, а юнит-тест такого класса требует
 * поднятого DI-контейнера.
 *
 * Проверка текстовая: ловит и вызов, и наследование `KoinComponent`.
 */
class DependencyInjectionTest {

    private val layerFiles = Konsist.scopeFromProduction().files
        .filter { file ->
            val path = file.path.replace('\\', '/')
            path.contains("/domain/") || path.contains("/data/")
        }

    @Test
    fun `layers do not resolve dependencies from the container`() {
        Coverage.report("DependencyInjectionTest", "файлы domain+data", layerFiles.size)

        layerFiles.assertTrue { file ->
            FORBIDDEN_LOCATOR_CALLS.none { call -> file.text.contains(call) }
        }
    }
}
