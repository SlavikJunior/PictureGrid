package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Раскладка `impl` по пакетам.
 *
 * Файл модуля `:feature:X:impl`, чей пакет заканчивается на `.impl`, обязан
 * лежать в подпакете: `presentation/ui`, `presentation/state`, `presentation/di`,
 * `data/...`, `navigation/`, `di/`. Исключение — точка входа модуля
 * (`<Feature>Module.kt`, `<feature>NavEntry.kt`).
 *
 * Зачем: на плоском пакете проверки, которые ищут код по пути (превью,
 * DI-локатор, состояние), молча не находят ничего и остаются зелёными.
 */
class PackageLayoutTest {

    @Test
    fun `impl files live in subpackages`() {
        val implRootFiles = Konsist.scopeFromProduction().files
            .filter { file -> file.packagee?.name?.endsWith(IMPL_PACKAGE_SUFFIX) == true }

        Coverage.report("PackageLayoutTest", "файлы в корне пакета .impl", implRootFiles.size)

        implRootFiles.assertTrue { file ->
            ALLOWED_IMPL_ROOT_FILES.any { suffix -> file.name.endsWith(suffix) }
        }
    }
}
