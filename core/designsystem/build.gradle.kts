plugins {
    id("convention.android.library")
    id("convention.android.compose")
}

android {
    namespace = "com.example.picturegrid.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
