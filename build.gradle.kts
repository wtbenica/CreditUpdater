plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("kapt") version "1.5.31"
    id("org.jetbrains.dokka") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    application
}

group = "dev.benica"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("com.beust:jcommander:1.82")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.dagger:dagger:2.46.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // mockito
    implementation("org.mockito:mockito-core:5.2.0")
    implementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    implementation("io.mockk:mockk:1.13.4")

    // junit
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")

    kapt("com.google.dagger:dagger-compiler:2.46.1")
    kaptTest("com.google.dagger:dagger-compiler:2.46.1")

    testImplementation("com.google.dagger:dagger:2.46.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.9.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.9.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("dev/benica/creditupdater/MainKt")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
    test {
        java.srcDirs("src/test/kotlin")
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "dev/benica/creditupdater/MainKt"
    }
    from("src/main/resources/sql") {
        include("**/*.sql")
    }
}

tasks.shadowJar {
    archiveBaseName.set("credit-updater")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "dev/benica/creditupdater/MainKt"
    }
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}

