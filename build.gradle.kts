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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("com.google.dagger", "dagger", "2.46.1")
    implementation("com.beust", "jcommander", "1.82")
    implementation("io.github.microutils", "kotlin-logging", "3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("mysql", "mysql-connector-java", "8.0.33")
    implementation(
        "org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.7.1"
    )
    // mockito
    implementation("org.mockito:mockito-core:5.2.0")
    implementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    // junit
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")

    kapt("com.google.dagger:dagger-compiler:2.46.1")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.9.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.9.2")
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
    from("src/main/sql") {
        include("**/*.sql")
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