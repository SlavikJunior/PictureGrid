package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-28 — у каждого `*Content` есть `@Preview`.
 *
 * Проверка ищет по имени функции, а не по пути: раскладка пакетов может быть
 * не приведена к стандарту (или приведена частично), и тогда фильтр по
 * `/presentation/` молча не находит ни одного экрана. Отсутствие раскладки —
 * это долг `PackageLayoutTest`, а не повод не проверять превью.
 *
 * `Screen`/Route превью не требует: он создаёт Store/ViewModel через DI и в
 * изоляции не отрендерится.
 */
class PreviewTest {

    private val productionFiles = Konsist.scopeFromProduction().files

    @Test
    fun `content composables have a preview`() {
        val contentCount = productionFiles.sumOf { file ->
            file.functions(includeNested = false, includeLocal = false)
                .count { function -> function.hasAnnotation("Composable") && function.name.endsWith("Content") }
        }
        Coverage.report("PreviewTest", "Composable-функции *Content", contentCount)

        productionFiles.assertTrue { file ->
            val functions = file.functions(includeNested = false, includeLocal = false)

            val contentNames = functions
                .filter { function -> function.hasAnnotation("Composable") }
                .map { it.name }
                .filter { name -> name.endsWith("Content") }

            val previewBodies = functions
                .filter { function -> function.hasAnnotation("Preview") }
                .map { it.text }

            contentNames.all { name -> previewBodies.any { body -> body.contains("$name(") } }
        }
    }
}

/**
 * Проверка по имени аннотации, без импорта типа: тестовый модуль не тянет
 * `androidx.compose.ui.tooling.preview` только ради проверки.
 */
private fun com.lemonappdev.konsist.api.provider.KoAnnotationProvider.hasAnnotation(
    name: String,
): Boolean = annotations.any { annotation -> annotation.name == name }
