package architecture.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin для Android-модуля-приложения.
 *
 * Применение в модуле:
 *   plugins { id("convention.android.application") }
 */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.android.application")
        target.configureAndroidModule()
    }
}
