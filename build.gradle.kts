import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openapi.generator") version "7.19.0"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.6"
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/contract/openapi.yaml")
    outputDir.set(layout.buildDirectory.asFile.get().resolve("generated/").path)
    apiPackage.set("io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api")
    modelPackage.set("io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "packageVersion" to "0.1.0",
            "reactive" to "true",
            "declarativeInterfaceReactiveMode" to "coroutines",
            "useFlowForArrayReturnType" to "false",
            "skipDefaultInterface" to "true"
        )
    )
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags = setOf("integration", "e2e")
    }
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

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    // Use the same test classes and classpath as the standard test task
    testClassesDirs = tasks.named<Test>("test").get().testClassesDirs
    classpath = tasks.named<Test>("test").get().classpath

    useJUnitPlatform {
        includeTags("integration")
    }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.check { dependsOn(integrationTest) }

kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.asFile.get().resolve("generated/src/main/kotlin"))
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}

group = "havonte1.github.com"
version = "0.1.0"
java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val resilience4jVersion = "2.3.0"

dependencies {
    // -------------------------------------------------
    // Spring Boot starters – runtime dependencies
    // -------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-envers")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop:4.0.0-M2")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // -------------------------------------------------
    // Kotlin & logging
    // -------------------------------------------------
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
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
    implementation("org.glassfish.jersey.core:jersey-common:3.1.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-all:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

    // -------------------------------------------------
    // Database drivers
    // -------------------------------------------------
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // -------------------------------------------------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.springframework.integration:spring-integration-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-toxiproxy")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-integration-test")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
