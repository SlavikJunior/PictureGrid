plugins {
    id("convention.android.library")
    id("convention.android.compose")
}

android {
    namespace = "com.example.picturegrid.core.navigation"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.material3)
}
