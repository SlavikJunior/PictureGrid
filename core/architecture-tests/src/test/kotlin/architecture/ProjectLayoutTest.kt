package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Проверки, которые относятся к этому проекту и не выражены стандартом.
 *
 * Канонические правила живут в шаблонах `arch-standard`; сюда попадает то,
 * что специфично для раскладки PictureGrid.
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
