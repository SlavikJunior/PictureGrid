package com.example.picturegrid.architecture

import androidx.compose.runtime.Composable
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.*
import org.junit.Test

/**
 * Архитектурные проверки Konsist.
 *
 * На этом этапе проверяются только соглашения, которым уже соответствует
 * текущий код. Правила про слои api/impl, репозитории и MVIKotlin-классы
 * появятся вместе с модулями (Epic 1) и дублируются Gradle-ограничениями
 * направлений зависимостей — Konsist их не заменяет.
 */
class ArchitectureKonsistTest {

    @Test
    fun `all classes and interfaces reside in the base package`() {
        Konsist
            .scopeFromProject()
            .classesAndInterfaces()
            .assertTrue { it.resideInPackage(BASE_PACKAGE) }
    }

    @Test
    fun `composable functions reside in ui, impl or designsystem packages`() {
        Konsist
            .scopeFromProject()
            .functions()
            .withAnnotationOf(Composable::class)
            .assertTrue {
                it.resideInPackage("..ui..") ||
                    it.resideInPackage("..impl..") ||
                    it.resideInPackage("..designsystem..")
            }
    }

    @Test
    fun `composable functions use PascalCase names`() {
        Konsist
            .scopeFromProject()
            .functions()
            .withAnnotationOf(Composable::class)
            .assertTrue { it.name.firstOrNull()?.isUpperCase() == true }
    }

    @Test
    fun `api modules do not contain composable functions`() {
        Konsist
            .scopeFromProject()
            .functions()
            .withAnnotationOf(Composable::class)
            .assertTrue { it.resideOutsidePackage("..api..") }
    }

    @Test
    fun `repository classes reside in impl or api packages`() {
        Konsist
            .scopeFromProject()
            .classes()
            .assertTrue { c ->
                !c.name.endsWith("Repository") ||
                    c.resideInPackage("..impl..") ||
                    c.resideInPackage("..api..")
            }
    }

    @Test
    fun `store classes reside in impl packages`() {
        Konsist
            .scopeFromProject()
            .classes()
            .assertTrue { c ->
                !c.name.endsWith("Store") ||
                    c.resideInPackage("..impl..")
            }
    }

    private companion object {
        const val BASE_PACKAGE = "com.example.picturegrid.."
    }
}