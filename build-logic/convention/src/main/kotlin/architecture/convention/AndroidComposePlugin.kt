package architecture.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin для Compose-модуля.
 *
 * Включает Compose и добавляет базовый набор зависимостей, общий для всех
 * UI-модулей проекта. Специфичные зависимости (material3, coil, navigation)
 * модуль объявляет сам.
 *
 * Применение в модуле:
 *   plugins { id("convention.android.compose") }
 */
class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.plugin.compose")

        // Порядок применения плагинов в модуле не важен: настройка ждёт
        // появления Android-плагина.
        target.plugins.withId("com.android.library") {
            target.configure<LibraryExtension> { buildFeatures.compose = true }
        }
        target.plugins.withId("com.android.application") {
            target.configure<ApplicationExtension> { buildFeatures.compose = true }
        }

        val libs = target.libs
        val bom = libs.findLibrary("compose-bom").get()

        target.dependencies {
            add("implementation", platform(bom))
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        }
    }
}
