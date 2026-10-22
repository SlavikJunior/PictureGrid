plugins {
    id("convention.android.application")
    id("convention.android.compose")
    id("convention.koin")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.example.picturegrid"

    defaultConfig {
        applicationId = "com.example.picturegrid"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                // R8 включён: release-сборка минифицируется и оптимизируется.
                enable = true
            }
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all { test ->
                // Robolectric требует доступа к внутренним модулям JVM на JDK 17+.
                test.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
}

roborazzi {
    // Разделяем output-каталоги по варианту: защита от гонки при Gradle 9 +
    // configuration cache (issue takahirom/roborazzi#830).
    separateOutputDirs.set(true)
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.mvikotlin)
    implementation(libs.mvikotlin.main)
    implementation(project(":feature:gallery:api"))
    implementation(project(":feature:gallery:impl"))
    implementation(project(":feature:viewer:api"))
    implementation(project(":feature:viewer:impl"))
    implementation(project(":core:images"))
    implementation(project(":core:network"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
