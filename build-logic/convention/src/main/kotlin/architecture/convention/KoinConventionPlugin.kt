package architecture.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin для Koin.
 *
 * Применяет Kotlin Compiler Plugin Koin, который генерирует модули
 * из аннотаций @Module/@Single. Требует, чтобы плагин был на classpath:
 * в корневом build.gradle.kts:
 *
 *   buildscript {
 *       dependencies { classpath("io.insert-koin:koin-compiler-gradle-plugin:<version>") }
 *   }
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("io.insert-koin.compiler.plugin")
    }
}
