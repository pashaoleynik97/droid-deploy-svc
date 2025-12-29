plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    api("org.springframework.data:spring-data-commons:3.4.2")
    api("org.springframework.boot:spring-boot:3.4.5")

    // OpenAPI/Swagger - for DTO annotations
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
}