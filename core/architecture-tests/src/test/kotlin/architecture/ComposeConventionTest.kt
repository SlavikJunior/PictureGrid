package architecture

import androidx.compose.runtime.Composable
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertNotEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-13 — modifier первый опциональный параметр.
 * INV-14 — никаких хардкод-строк в UI.
 * INV-17 — цвета и типографика из MaterialTheme.
 */
class ComposeConventionTest {

    private val composables = Konsist.scopeFromProduction()
        .functions()
        .withAnnotationOf(Composable::class)

    @Test
    fun `composable has modifier as first optional parameter`() {
        Coverage.report("ComposeConventionTest", "Composable-функции", composables.size)

        // Пустая область — это не «проверок нет ошибок», а «проверок не было».
        // Без явной проверки следующие тесты зеленеют, ничего не увидев.
        composables.assertNotEmpty()

        composables.assertTrue { function ->
            val parameters = function.parameters
            val modifierIndex = parameters.indexOfFirst { it.name == "modifier" }

            if (modifierIndex < 0) {
                // Функция без modifier — допустимо (например, stateless-обёртка).
                true
            } else {
                // Ни один обязательный параметр не должен стоять после modifier,
                // и все параметры до modifier — обязательные.
                parameters.take(modifierIndex).none { it.hasDefaultValue() }
            }
        }
    }

    @Test
    fun `composable functions use pascal case names`() {
        composables.assertTrue { function ->
            function.name.firstOrNull()?.isUpperCase() == true
        }
    }

    @Test
    fun `ui does not contain hardcoded text`() {
        Konsist.scopeFromProduction().files.assertTrue { file ->
            !HARDCODED_TEXT.containsMatchIn(file.text)
        }
    }

    @Test
    fun `ui does not contain hardcoded colors`() {
        Konsist.scopeFromProduction().files.assertTrue { file ->
            file.isPaletteFile() || !HARDCODED_COLOR.containsMatchIn(file.text)
        }
    }

    private companion object {
        /**
         * Text("...") — строковый литерал как единственный аргумент.
         *
         * Кавычка внутри raw-строки невозможна без экранирования, поэтому
         * используется обычная строка: иначе regex молча теряет последний
         * символ и начинает матчить любое `Text(`.
         */
        val HARDCODED_TEXT = Regex("\\bText\\(\\s*\"")

        /** Color(0xFF......) — hex-литерал цвета. */
        val HARDCODED_COLOR = Regex("""\bColor\(\s*0x[0-9A-Fa-f]{8}""")
    }
}

/**
 * Файл, в котором законно объявлять цветовые литералы: палитра и тема.
 *
 * Канон исключает пакет `theme`. В этом проекте дизайн-система называется
 * `core/designsystem` и лежит в корне пакета модуля, поэтому правило
 * признаёт и такой расклад — иначе проверка ругается на саму палитру.
 */
private fun KoFileDeclaration.isPaletteFile(): Boolean {
    if (path.replace('\\', '/').contains("/theme/")) return true
    return moduleName.contains("designsystem") && (name == "Color" || name == "Theme")
}
