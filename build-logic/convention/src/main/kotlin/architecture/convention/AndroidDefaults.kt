package architecture.convention

import org.gradle.api.JavaVersion

/**
 * Значения по умолчанию для Android-модулей.
 *
 * Правь под проект в одном месте здесь, а не копипастой по модулям (INV-24).
 */
object AndroidDefaults {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    val JVM: JavaVersion = JavaVersion.VERSION_11
}
