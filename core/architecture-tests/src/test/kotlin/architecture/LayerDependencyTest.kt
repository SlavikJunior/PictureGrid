package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-04 — domain не зависит ни от чего в проекте.
 * INV-10 — однонаправленный поток данных.
 *
 * Направления зависимостей между слоями:
 *   presentation → domain
 *   data        → domain
 *   domain      → ничего
 *
 * Проверка сделана через импорты, а не через `assertArchitecture`:
 * тот требует наличия всех трёх слоёв в проекте и падает с исключением,
 * если слой ещё не создан. Здесь проверки мягкие и дают понятный отчёт.
 */
class LayerDependencyTest {

    private val productionScope = Konsist.scopeFromProduction()

    private val domainScope = Konsist.scopeFromPackage(DOMAIN_PACKAGE)
    private val dataScope = Konsist.scopeFromPackage(DATA_PACKAGE)
    private val presentationScope = Konsist.scopeFromPackage(PRESENTATION_PACKAGE)

    @Test
    fun `domain does not depend on data or presentation`() {
        Coverage.report("LayerDependencyTest", "файлы domain+data+presentation", domainScope.files.size + dataScope.files.size + presentationScope.files.size)

        domainScope.files.assertTrue { file ->
            file.imports.none { import ->
                import.name.contains(".data.") || import.name.contains(".presentation.")
            }
        }
    }

    @Test
    fun `data does not depend on presentation`() {
        dataScope.files.assertTrue { file ->
            file.imports.none { import -> import.name.contains(".presentation.") }
        }
    }

    @Test
    fun `presentation does not depend on data directly`() {
        presentationScope.files.assertTrue { file ->
            file.imports.none { import -> import.name.contains(".data.") }
        }
    }

    @Test
    fun `domain does not depend on other project modules`() {
        domainScope.files.assertTrue { file ->
            file.imports.none { import ->
                import.name.startsWith(BASE_PACKAGE) &&
                    !import.name.contains(".domain.")
            }
        }
    }

    @Test
    fun `data does not depend on other feature modules`() {
        dataScope.files.assertTrue { file ->
            file.imports.none { import ->
                import.name.contains(".feature.") && !import.name.contains(".domain.")
            }
        }
    }
}
