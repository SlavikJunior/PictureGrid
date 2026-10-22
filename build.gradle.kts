// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("convention.arch.module-graph")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.koin.compiler) apply false
}
