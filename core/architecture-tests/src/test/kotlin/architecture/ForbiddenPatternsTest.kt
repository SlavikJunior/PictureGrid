package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-21 — нет GlobalScope, !! и пустых catch в production-коде.
 *
 * Проверки текстовые: они эвристические и могут давать ложные срабатывания,
 * поэтому строки-комментарии и импорты отфильтровываются.
 */
class ForbiddenPatternsTest {

    private val productionScope = Konsist.scopeFromProduction()

    @Test
    fun `no GlobalScope in production code`() {
        productionScope.files.assertTrue { file ->
            !file.text.contains("GlobalScope")
        }
    }

    @Test
    fun `no not-null assertion operator in production code`() {
        productionScope.files.assertTrue { file ->
            file.codeLines().none { it.contains("!!") }
        }
    }

    @Test
    fun `no runBlocking in production code`() {
        productionScope.files.assertTrue { file ->
            !file.text.contains("runBlocking")
        }
    }

    @Test
    fun `no empty catch block in production code`() {
        productionScope.files.assertTrue { file ->
            !file.text.contains(Regex("""catch\s*\([^)]*\)\s*\{\s*\}"""))
        }
    }
}

/** Строки кода без комментариев, импортов и строковых литералов. */
private fun KoFileDeclaration.codeLines(): List<String> =
    text.lines()
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .filterNot { it.startsWith("//") }
        .filterNot { it.startsWith("*") }
        .filterNot { it.startsWith("/*") }
        .filterNot { it.startsWith("import ") }
        .filterNot { it.startsWith("\"") }
