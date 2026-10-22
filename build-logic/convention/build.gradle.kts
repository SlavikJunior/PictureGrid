plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "architecture.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(gradleApi())
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "convention.android.application"
            implementationClass = "architecture.convention.AndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = "convention.android.library"
            implementationClass = "architecture.convention.AndroidLibraryPlugin"
        }
        register("androidCompose") {
            id = "convention.android.compose"
            implementationClass = "architecture.convention.AndroidComposePlugin"
        }
        register("koin") {
            id = "convention.koin"
            implementationClass = "architecture.convention.KoinConventionPlugin"
        }
        register("archModuleGraph") {
            id = "convention.arch.module-graph"
            implementationClass = "architecture.convention.ArchModuleGraphPlugin"
        }
    }
}
