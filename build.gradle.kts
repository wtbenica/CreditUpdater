import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("kapt") version "1.5.31"
    application
    id("org.jetbrains.dokka") version "1.5.31"
}

group = "me.wbenica"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(group = "com.google.dagger", name = "dagger", version = "2.46.1")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "3.0.5")
    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.33")
    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = "1.7.1"
    )

    kapt("com.google.dagger:dagger-compiler:2.38.1")

    testImplementation(dependencyNotation = "org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly(dependencyNotation = "org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

kapt {
    correctErrorTypes = true
}

application {
    mainClass.set("MainKt")
}