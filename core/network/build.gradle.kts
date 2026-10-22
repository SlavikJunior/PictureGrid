plugins {
    id("convention.android.library")
}

android {
    namespace = "com.example.picturegrid.core.network"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.okhttp)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
