plugins {
    id("convention.android.library")
}

kotlin {
    // INV-26: публичный API api-модуля объявлен явно.
    explicitApi()
}

android {
    namespace = "com.example.picturegrid.feature.gallery.api"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
