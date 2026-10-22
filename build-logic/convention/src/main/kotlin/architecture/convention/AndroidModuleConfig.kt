package architecture.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.api.tasks.testing.Test

/**
 * Общая конфигурация Android-модулей.
 *
 * Значения по умолчанию — в [AndroidDefaults]. Модуль задаёт только то, что
 * действительно отличается: namespace, свои зависимости и тестовые опции.
 */
fun Project.configureAndroidModule() {
    val isLibrary = plugins.hasPlugin("com.android.library")

    if (isLibrary) {
        configure<LibraryExtension> {
            compileSdk = AndroidDefaults.COMPILE_SDK

            defaultConfig {
                minSdk = AndroidDefaults.MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = AndroidDefaults.JVM
                targetCompatibility = AndroidDefaults.JVM
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    } else {
        configure<ApplicationExtension> {
            compileSdk = AndroidDefaults.COMPILE_SDK

            defaultConfig {
                minSdk = AndroidDefaults.MIN_SDK
                targetSdk = AndroidDefaults.TARGET_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = AndroidDefaults.JVM
                targetCompatibility = AndroidDefaults.JVM
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }

    // Robolectric: каждый тест-класс — в своей JVM. Иначе сандбоксы
    // Robolectric внутри одной JVM гоняются за native runtime, и тест
    // падает с UnsatisfiedLinkError (воспроизводится и без этих правок).
    tasks.withType(Test::class.java).configureEach {
        forkEvery = 1
    }
}

/** Доступ к version catalog из convention plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
