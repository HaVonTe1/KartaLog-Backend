plugins {
    id("org.openapi.generator") version "7.19.0"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.6"
}

openApiGenerate {
    generatorName.set("kotlin-server")
    inputSpec.set("$rootDir/contract/openapi.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("com.github.havonte1.adapter.inbound.rest.api")
    modelPackage.set("com.github.havonte1.adapter.inbound.rest.model")
    configOptions.set(mapOf(
        "library" to "jaxrs-spec",
        "interfaceOnly" to "true",
        "useJakartaEe" to "true"
    ))
}

sourceSets["main"].java.srcDir("$buildDir/generated/src/main/kotlin")



tasks.withType<Test> {
    useJUnitPlatform()
    // Show stdout/stderr from tests (including logger output) even when Gradle runs in quiet mode
    testLogging {
        // Log the result of each test
        events("passed", "skipped", "failed")
        // Show any println or logger output to the console
        showStandardStreams = true
        // Show full exception details
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

kotlin {
    jvmToolchain(17)
}

group = "havonte1.github.com"
version = "0.1.0"
java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // -------------------------------------------------
    // Spring Boot starters – runtime dependencies
    // -------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.integration:spring-integration-jpa")
    implementation("org.springframework.integration:spring-integration-http")

    // -------------------------------------------------
    // Kotlin & logging
    // -------------------------------------------------
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // -------------------------------------------------
    // Playwright & HTML parsing
    // -------------------------------------------------
    implementation("com.microsoft.playwright:playwright:1.58.0")
    implementation("org.jsoup:jsoup:1.22.1")

    // -------------------------------------------------
    // Jakarta APIs
    // -------------------------------------------------
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")

    // -------------------------------------------------
    // Database drivers
    // -------------------------------------------------
    runtimeOnly("org.postgresql:postgresql")


    // -------------------------------------------------
    // Optional dev tools (runtime)
    // -------------------------------------------------
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.springframework.boot:spring-boot-docker-compose")

    // -------------------------------------------------
    // Annotation processing (optional, compile‑only)
    // -------------------------------------------------
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // -------------------------------------------------
    // TEST DEPENDENCIES – full list from original boot pom
    // -------------------------------------------------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.integration:spring-integration-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-integration-test")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
