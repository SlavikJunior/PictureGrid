package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-18 — маршруты Navigation 3.
 *
 * Каждый файл `navigation/<Name>Route.kt` объявляет ровно тип-ключ, и этот тип:
 * - помечен `@Serializable` — иначе ключ не переживёт восстановление back stack;
 * - реализует `NavKey` (или проектный алиас из [NAV_KEY_TYPES]) — иначе Nav3
 *   его не примет.
 *
 * Файлы `<feature>NavEntry.kt` не проверяются: в них функции, а не типы.
 */
class NavigationConventionTest {

    private val routeFiles = Konsist.scopeFromProduction().files
        .filter { file ->
            val path = file.path.replace('\\', '/')
            path.contains("/navigation/") && file.name.endsWith("Route")
        }

    @Test
    fun `route keys are serializable NavKey`() {
        Coverage.report("NavigationConventionTest", "файлы *Route.kt", routeFiles.size)

        routeFiles.assertTrue { file ->
            val classes = file.classes(includeNested = false, includeLocal = false)
            val objects = file.objects(includeNested = false)
            val declared = classes.size + objects.size

            declared > 0 &&
                classes.all { it.hasRouteAnnotation() && it.hasNavKeyParent() } &&
                objects.all { it.hasRouteAnnotation() && it.hasNavKeyParent() }
        }
    }
}

private fun com.lemonappdev.konsist.api.declaration.KoClassDeclaration.hasRouteAnnotation(): Boolean =
    annotations.any { annotation -> annotation.name == "Serializable" }

private fun com.lemonappdev.konsist.api.declaration.KoObjectDeclaration.hasRouteAnnotation(): Boolean =
    annotations.any { annotation -> annotation.name == "Serializable" }

private fun com.lemonappdev.konsist.api.declaration.KoClassDeclaration.hasNavKeyParent(): Boolean =
    parents().any { parent -> parent.name in NAV_KEY_TYPES }

private fun com.lemonappdev.konsist.api.declaration.KoObjectDeclaration.hasNavKeyParent(): Boolean =
    parents().any { parent -> parent.name in NAV_KEY_TYPES }
