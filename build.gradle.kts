// Standalone root. Not read when regatta builds inside mxmariner, so nothing here may be needed
// by the projects: their task wiring, image and deploy tasks live in server/build.gradle.kts.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
