// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

// OneDrive can lock Android's short-lived intermediate files during sync.
val localBuildRoot = gradle.gradleUserHomeDir.resolve("studyreader-build/${rootProject.name}")
layout.buildDirectory.set(localBuildRoot.resolve("root"))
subprojects { layout.buildDirectory.set(localBuildRoot.resolve(name)) }
