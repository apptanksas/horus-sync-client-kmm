plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    //alias(libs.plugins.kotlinxSerialization)
    kotlin("plugin.serialization") version "2.0.20"
    id("maven-publish")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

         // Publish android variants
        // publishLibraryVariants("release", "debug")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "core"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.client.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.crypto)
        }
        commonTest.dependencies {
            implementation(libs.test.kotlin)
            implementation(libs.test.ktor)
            implementation(libs.test.sqldelight)
        }
        // Android dependencies
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
        }
        // IOS dependencies
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.ios)
        }
    }
}

android {
    namespace = "com.apptank.horus.client"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

group = "com.apptank.horus.client"
version = "0.0.0"
