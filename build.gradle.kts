import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("kapt") version "1.5.31"
    application
    id("org.jetbrains.dokka") version "1.5.31"
}

group = "benica.dev"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = ".MainKt"
    }
    from(sourceSets.main.get().output.classesDirs)
}

kapt {
    correctErrorTypes = true
}

application {
    mainClass.set("MainKt")
}
