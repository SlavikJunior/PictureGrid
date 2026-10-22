plugins {
    id("convention.android.library")
    id("convention.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.picturegrid.core.navigation"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.material3)
}
