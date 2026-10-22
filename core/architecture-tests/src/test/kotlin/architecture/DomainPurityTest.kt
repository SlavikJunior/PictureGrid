package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.provider.KoAnnotationProvider
import com.lemonappdev.konsist.api.verify.assertNotEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-05 — domain не использует платформенные классы.
 * INV-06 — domain не знает про инфраструктуру.
 *
 * Проверяет, что слой `domain` остаётся чистым Kotlin-кодом: без `android.*`,
 * `java.*`, без библиотек хранения/сети/DI/UI и без framework-аннотаций
 * (`@Serializable`, `@Entity`, `@Parcelize`).
 */
class DomainPurityTest {

    private val productionScope = Konsist.scopeFromProduction()

    private val domainScope = Konsist.scopeFromPackage(DOMAIN_PACKAGE)

    @Test
    fun `project has a domain layer`() {
        // Пока в проекте нет фич, доменного слоя нет и быть не должно:
        // за требованием «домен существует» следим с момента появления первой фичи.
        val hasFeatures = productionScope.files.any { file ->
            file.moduleName?.contains("feature") == true
        }
        if (!hasFeatures) return

        domainScope.files.assertNotEmpty()
    }

    @Test
    fun `domain layer does not use platform classes`() {
        Coverage.report("DomainPurityTest", "файлы domain", domainScope.files.size)

        domainScope.files.assertTrue { file ->
            file.imports.none { import ->
                import.name.startsWith("android.") ||
                    import.name.startsWith("java.") ||
                    import.name.startsWith("javax.")
            }
        }
    }

    @Test
    fun `domain layer does not use infrastructure libraries`() {
        domainScope.files.assertTrue { file ->
            file.imports.none { import ->
                FORBIDDEN_IN_DOMAIN.any { prefix -> import.name.startsWith(prefix) }
            }
        }
    }

    @Test
    fun `domain layer does not declare composables`() {
        domainScope.files.assertTrue { file ->
            !file.text.contains("@Composable")
        }
    }

    @Test
    fun `domain layer has no framework annotations`() {
        domainScope.files.assertTrue { file ->
            file.classes(includeNested = true, includeLocal = false).none { it.hasForbiddenAnnotation() } &&
                file.interfaces(includeNested = true).none { it.hasForbiddenAnnotation() } &&
                file.objects(includeNested = true).none { it.hasForbiddenAnnotation() } &&
                file.properties(includeNested = true).none { it.hasForbiddenAnnotation() }
        }
    }
}

/**
 * Аннотация фреймворка в `domain` — нарушение INV-06, даже если импорт скрыт
 * за `typealias`.
 */
private fun KoAnnotationProvider.hasForbiddenAnnotation(): Boolean =
    annotations.any { annotation -> annotation.name in FORBIDDEN_ANNOTATIONS_IN_DOMAIN }
