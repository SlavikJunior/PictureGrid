package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withPublicOrDefaultModifier
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-26 — минимальная видимость: в `impl` всё `internal`.
 * INV-27 — единственный публичный экспорт `impl` — `<feature>NavEntry`.
 *
 * В Kotlin объявление без модификатора видимости — `public`, поэтому проверка
 * идёт по `hasPublicOrDefaultModifier`, а не по наличию слова `public`.
 *
 * Проверяются только верхнеуровневые объявления: вложенные (`sealed interface`
 * со `data class` внутри) наследуют видимость контейнера и нарушением не являются.
 */
class VisibilityTest {

    private val productionScope = Konsist.scopeFromProduction()

    /** Файлы модулей `:feature:X:impl` (`feature:cart:impl` или `feature-cart-impl`). */
    private val implFiles = productionScope.files
        .filter { file -> file.moduleName?.contains("impl") == true }

    @Test
    fun `impl exposes only nav entry`() {
        Coverage.report("VisibilityTest", "файлы модулей impl", implFiles.size)

        implFiles.assertTrue { file ->
            val publicNames: List<String> =
                file.classes(includeNested = false, includeLocal = false)
                    .withPublicOrDefaultModifier().map { it.name } +
                    file.interfaces(includeNested = false)
                        .withPublicOrDefaultModifier().map { it.name } +
                    file.objects(includeNested = false)
                        .withPublicOrDefaultModifier().map { it.name } +
                    file.functions(includeNested = false, includeLocal = false)
                        .withPublicOrDefaultModifier().map { it.name } +
                    file.properties(includeNested = false)
                        .withPublicOrDefaultModifier().map { it.name }

            publicNames.all { name ->
                ALLOWED_PUBLIC_SUFFIXES.any { suffix -> name.endsWith(suffix) }
            }
        }
    }
}
