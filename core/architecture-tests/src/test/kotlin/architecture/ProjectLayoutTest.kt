package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Проверки раскладки, не покрытые остальными тестами канона.
 *
 * Правила общие для всех проектов: Composable — в `ui`/`impl`/`designsystem`,
 * в `api`-модулях Composable нет, репозиторий — в `impl` или `api`, держатель
 * состояния с суффиксом `Store` — в `impl`. Последнее правило действует только
 * там, где такой суффикс используется (например, MVIKotlin), и в остальных
 * проектах ничего не проверяет.
 */
class ProjectLayoutTest {

    private val productionScope = Konsist.scopeFromProduction()

    @Test
    fun `composable functions reside in ui, impl or designsystem packages`() {
        Coverage.report("ProjectLayoutTest", "Composable-функции", productionScope.functions().size)

        productionScope.functions()
            .withAnnotationOf(androidx.compose.runtime.Composable::class)
            .assertTrue { function ->
                function.resideInPackage("..ui..") ||
                    function.resideInPackage("..impl..") ||
                    function.resideInPackage("..designsystem..")
            }
    }

    @Test
    fun `api modules do not contain composable functions`() {
        productionScope.functions()
            .withAnnotationOf(androidx.compose.runtime.Composable::class)
            .assertTrue { function ->
                function.resideOutsidePackage("..api..")
            }
    }

    @Test
    fun `repository classes reside in impl or api packages`() {
        productionScope.classes()
            .assertTrue { declaration ->
                !declaration.name.endsWith("Repository") ||
                    declaration.resideInPackage("..impl..") ||
                    declaration.resideInPackage("..api..")
            }
    }

    @Test
    fun `store classes reside in impl packages`() {
        productionScope.classes()
            .assertTrue { declaration ->
                !declaration.name.endsWith("Store") ||
                    declaration.resideInPackage("..impl..")
            }
    }
}
