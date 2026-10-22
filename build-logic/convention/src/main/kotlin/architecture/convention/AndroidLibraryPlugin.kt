package architecture.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin для Android-модуля-библиотеки.
 *
 * Применение в модуле:
 *   plugins { id("convention.android.library") }
 */
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.android.library")
        target.configureAndroidModule()
    }
}
