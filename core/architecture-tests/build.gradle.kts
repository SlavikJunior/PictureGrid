plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))

    // Нужны только для проверки аннотаций и типов в Konsist-тестах.
    // Compose-версии задаёт BOM, поэтому подключаем его как platform.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.runtime)
    testImplementation(libs.androidx.lifecycle.viewmodel)
}

tasks.test {
    // Konsist обходит весь проект от корня — нужен стабильный working directory.
    workingDir = rootProject.projectDir

    // Konsist читает исходники всего проекта в рантайме, поэтому Gradle не
    // считает их входами этой задачи. Без этого после первой сборки задача
    // навсегда становится UP-TO-DATE и проверки молча не выполняются —
    // `./gradlew check` при этом зелёный.
    outputs.upToDateWhen { false }

    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
