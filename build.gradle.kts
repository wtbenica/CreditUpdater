import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    application
    kotlin("kapt") version "1.5.31"
    id("org.jetbrains.dokka") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "dev.benica"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib", "1.8.10"))
    implementation(group = "com.google.dagger", name = "dagger", version = "2.46.1")
    implementation(group = "com.beust", name = "jcommander", version = "1.81")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "3.0.5")
    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.33")
    implementation(kotlin("stdlib"))
    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = "1.7.1"
    )

    implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")

    kapt("com.google.dagger:dagger-compiler:2.46.1")

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.8.1")
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

tasks.shadowJar {
    archiveBaseName.set("credit-updater")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    mergeServiceFiles()
}