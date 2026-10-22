package architecture.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Общая конфигурация Android-модулей.
 *
 * Значения по умолчанию — в `AndroidDefaults`.
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

            buildFeatures {
                compose = true
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

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }

            buildFeatures {
                compose = true
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }

    configureKotlin()
}

fun Project.configureKotlin() {
    configure<KotlinAndroidProjectExtension> {
        jvmToolchain(21)
    }
}

/** Доступ к version catalog из convention plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
