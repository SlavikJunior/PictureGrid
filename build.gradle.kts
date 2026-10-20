// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}

fun checkModuleDependencies(project: Project) {
    val forbidden = mapOf(
        ":feature:gallery:api" to setOf(":feature:gallery:impl", ":feature:viewer:impl"),
        ":feature:viewer:api" to setOf(":feature:viewer:impl", ":feature:gallery:impl"),
        ":feature:gallery:impl" to setOf(":feature:viewer:impl"),
        ":feature:viewer:impl" to setOf(":feature:gallery:impl"),
        ":core:common" to setOf(":feature:gallery:api", ":feature:gallery:impl", ":feature:viewer:api", ":feature:viewer:impl"),
        ":core:network" to setOf(":feature:gallery:api", ":feature:gallery:impl", ":feature:viewer:api", ":feature:viewer:impl"),
        ":core:images" to setOf(":feature:gallery:api", ":feature:gallery:impl", ":feature:viewer:api", ":feature:viewer:impl"),
        ":core:navigation" to setOf(":feature:gallery:api", ":feature:gallery:impl", ":feature:viewer:api", ":feature:viewer:impl"),
        ":core:designsystem" to setOf(":feature:gallery:api", ":feature:gallery:impl", ":feature:viewer:api", ":feature:viewer:impl"),
    )
    val forbiddenModules = forbidden[project.path] ?: return
    val violations = mutableListOf<String>()
    project.configurations.forEach { config ->
        config.dependencies.filterIsInstance<ProjectDependency>().forEach { dep ->
            val depPath = dep.path
            if (depPath in forbiddenModules) {
                violations.add("${project.path} -> $depPath (via ${config.name})")
            }
        }
    }
    if (violations.isNotEmpty()) {
        throw GradleException("Forbidden module dependencies:\n" + violations.joinToString("\n"))
    }
}

subprojects {
    afterEvaluate {
        checkModuleDependencies(this)
    }
}

tasks.register("checkModuleDependencies") {
    notCompatibleWithConfigurationCache("Uses Project references for dependency graph inspection")
    group = "verification"
    description = "Verifies module dependency directions per ARCHITECTURE.md"
    doLast {
        subprojects.forEach { checkModuleDependencies(it) }
    }
}
