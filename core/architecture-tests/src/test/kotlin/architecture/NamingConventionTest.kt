package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-25 — суффиксы имён соответствуют своей роли и лежат в правильном слое.
 *
 * Проверки намеренно мягкие: они ловят очевидные ошибки, а не спорные случаи.
 */
class NamingConventionTest {

    private val productionScope = Konsist.scopeFromProduction()

    @Test
    fun `use cases reside in domain layer`() {
        productionScope.classes()
            .withNameEndingWith(Suffixes.USE_CASE)
            .assertTrue { it.resideInPackage(DOMAIN_PACKAGE) }
    }

    @Test
    fun `dtos reside in data layer`() {
        productionScope.classes()
            .withNameEndingWith(Suffixes.DTO)
            .assertTrue { it.resideInPackage(DATA_PACKAGE) }
    }

    @Test
    fun `entities reside in data layer`() {
        productionScope.classes()
            .withNameEndingWith(Suffixes.ENTITY)
            .assertTrue {
                // Слой данных — это не только пакет `..data..`: в проектах с
                // выделенными модулями (`core:database`, `core:local`) сущности
                // лежат в самом модуле. Все три варианта — один и тот же слой.
                it.resideInPackage(DATA_PACKAGE) ||
                    it.resideInPackage("..database..") ||
                    it.resideInPackage("..local..")
            }
    }

    @Test
    fun `ui state event and effect reside in presentation layer`() {
        productionScope.classes()
            .withNameEndingWith(Suffixes.UI_STATE, Suffixes.UI_EVENT, Suffixes.UI_EFFECT)
            .assertTrue {
                it.resideInPackage(PRESENTATION_PACKAGE) || it.resideInPackage("..state..")
            }
    }

    @Test
    fun `repository contracts reside in domain or api package`() {
        productionScope.interfaces()
            .withNameEndingWith(Suffixes.REPOSITORY)
            .assertTrue {
                it.resideInPackage(DOMAIN_PACKAGE) ||
                    it.resideInPackage("..repository..") ||
                    it.resideInPackage("..api..")
            }
    }

    @Test
    fun `file name matches the primary type name`() {
        productionScope.files.assertTrue { file ->
            // Интерфейсы обязательны в этом списке: файл с `sealed interface Node`
            // иначе считается «не имеющим совпадающего типа», хотя имя совпадает.
            val types = file.classes().map { it.name } +
                file.interfaces().map { it.name } +
                file.objects().map { it.name }
            // Имя файла в Konsist — без расширения.
            types.isEmpty() || types.any { type -> file.name == type }
        }
    }
}
