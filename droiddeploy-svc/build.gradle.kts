plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":droiddeploy-core"))
    implementation(project(":droiddeploy-db"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Security for password encoding
    implementation("org.springframework.security:spring-security-crypto")

    // Kotlin logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Database driver
    runtimeOnly("org.postgresql:postgresql")

    // Flyway for database migrations (using Spring Boot starter for proper autoconfiguration)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}