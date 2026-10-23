plugins {
    id("convention.android.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // INV-26: публичный API api-модуля объявлен явно.
    explicitApi()
}

android {
    namespace = "com.example.picturegrid.feature.gallery.api"
}

dependencies {
    // Маршрут экрана живёт в контракте фичи, поэтому зависимость
    // публикуется наружу (INV-18). Платформа выравнивает транзитивные
    // версии Compose: без неё модуль резолвит их по POM navigation3.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
