plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.picturegrid.core.common"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}