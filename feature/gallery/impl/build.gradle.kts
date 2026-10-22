plugins {
    id("convention.android.library")
    id("convention.android.compose")
    id("convention.koin")
}

android {
    namespace = "com.example.picturegrid.feature.gallery.impl"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }
}

dependencies {
    implementation(project(":feature:gallery:api"))
    implementation(project(":core:network"))
    implementation(project(":core:navigation"))
    implementation(project(":core:images"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mvikotlin)
    implementation(libs.mvikotlin.main)
    implementation(libs.mvikotlin.extensions.coroutines)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(project(":core:designsystem"))
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.material3)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.work.testing)
}
