import com.vanniktech.maven.publish.SonatypeHost

val libGroupId = project.findProperty("lib.groupId") as String
val libArtifactId = project.findProperty("lib.artifactId") as String
val libVersion = project.findProperty("lib.version") as String


group = libGroupId
version = libVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "2.0.20"
    id("maven-publish")
    id("app.cash.sqldelight") version "2.0.2"
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
    // To publish the library to the maven repository
    id("com.vanniktech.maven.publish") version "0.29.0"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        // Publish android variants
        publishLibraryVariants("release", "debug")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Horus is a library data synchronizer between app and remote server"
        homepage = "Link [TBD]"
        version = libVersion
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "horus"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.client.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.crypto)
            implementation(libs.storage.settings)
        }
        commonTest.dependencies {
            implementation(libs.test.kotlin)
            implementation(libs.test.ktor)
            implementation(libs.test.mockative)
            implementation(libs.test.storage.settings)
        }
        // Android dependencies
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.android)
            implementation(libs.test.sqldelight)
        }
        androidUnitTest.dependencies {
            implementation(libs.android.test.robolectric)
        }
        // IOS dependencies
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.driver.ios)
        }
    }
}

android {
    namespace = "org.apptank.horus"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Configuration mockative
    configurations
        .filter { it.name.startsWith("ksp") && it.name.contains("Test") }
        .forEach {
            add(it.name, libs.ksp.mockative)
        }
}

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// PUBLISH CONFIGURATION TO MAVEN REPOSITORY
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = libGroupId,
        artifactId = libArtifactId,
        version = libVersion
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("Horus sync client library")
        description.set("Horus is a client library for Kotlin Multiplatform aimed at providing an easy and simple way to store data locally and synchronize it with a remote server, ensuring data security and integrity.")
        inceptionYear.set("2024")
        url.set("https://github.com/apptanksas/horus-sync-client-kmm")


        licenses {
            license {
                name.set("AGPL-3.0-only")
                url.set("https://opensource.org/license/agpl-v3")
            }
        }

        // Specify developer information
        developers {
            developer {
                id.set("jhospina")
                name.set("John Ospina")
                email.set("johnospina.apptank@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/apptanksas/horus-sync-client-kmm")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}
