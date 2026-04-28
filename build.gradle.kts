// Top-level build file where you can add configuration options common to all sub-projects/modules.
group = "ARC"
version = "3.01"

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.cyclonedx.bom") version "3.2.2"
}