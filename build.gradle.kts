plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("kapt") version "2.2.10"
    id("com.gradleup.shadow") version "8.3.9"
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
    implementation("com.google.dagger:dagger:2.57.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // mockito
    implementation("org.mockito:mockito-core:5.14.2")
    implementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    implementation("io.mockk:mockk:1.13.4")

    // junit
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")

    kapt("com.google.dagger:dagger-compiler:2.57.1")
    kaptTest("com.google.dagger:dagger-compiler:2.57.1")

    testImplementation("com.google.dagger:dagger:2.57.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.9.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.9.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
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

// The tests that need a MariaDB server are tagged "db": `test` is the fast ones, `integrationTest` the rest
tasks.test {
    useJUnitPlatform { excludeTags("db") }
    systemProperty("creditupdater.progressFile", layout.buildDirectory.file("test-progress.json").get().asFile.path)
    doLast { delete(layout.buildDirectory.file("test-progress.json")) }
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs the tests that need a MariaDB server (tagged \"db\")."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("db") }
    // so the tests never overwrite the progress file of a real run
    systemProperty("creditupdater.progressFile", layout.buildDirectory.file("test-progress.json").get().asFile.path)
    doLast { delete(layout.buildDirectory.file("test-progress.json")) }
    shouldRunAfter(tasks.test)
}

