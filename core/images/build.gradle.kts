plugins {
    id("convention.android.library")
}

android {
    namespace = "com.example.picturegrid.core.images"
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            )
        }
    }
}

dependencies {
    implementation(project(":core:network"))
    // LocalImageLoader публикует тип ProvidableCompositionLocal наружу.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.compose.runtime)
    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
