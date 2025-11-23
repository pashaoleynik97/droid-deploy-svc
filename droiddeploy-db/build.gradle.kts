plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// This is a library module, not an executable, so we disable bootJar and enable regular jar
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    implementation(project(":droiddeploy-core"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kotlin logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Database dependencies
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
