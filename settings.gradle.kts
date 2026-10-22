pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
includeBuild("build-logic")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PictureGrid"
include(":app")
include(":core:network")
include(":core:images")
include(":core:navigation")
include(":core:designsystem")
include(":core:architecture-tests")
include(":feature:gallery:api")
include(":feature:gallery:impl")
include(":feature:viewer:api")
include(":feature:viewer:impl")