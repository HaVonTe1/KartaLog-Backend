import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.kotlin.jpa)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/contract/openapi.yaml")
    outputDir.set(
        layout.buildDirectory.asFile
            .get()
            .resolve("generated/")
            .path,
    )
    apiPackage.set("io.github.havonte1.kartalog.backend.adapter.inbound.rest.api")
    modelPackage.set("io.github.havonte1.kartalog.backend.adapter.inbound.rest.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "packageVersion" to "0.1.0",
            "reactive" to "true",
            "declarativeInterfaceReactiveMode" to "coroutines",
            "useFlowForArrayReturnType" to "false",
            "skipDefaultInterface" to "true",
        ),
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

val integrationTest =
    tasks.register<Test>("integrationTest") {
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
            kotlin.srcDir(
                layout.buildDirectory.asFile
                    .get()
                    .resolve("generated/src/main/kotlin"),
            )
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

detekt {
    buildUponDefaultConfig = true
    allRules = false
}

tasks.named("detekt") {
    enabled = false
}

ktlint {
    version.set(libs.versions.ktlintTool)
    android.set(false)
    outputToConsole.set(true)
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    enabled = false
}

tasks.named("runKtlintFormatOverMainSourceSet") {
    enabled = false
}

tasks.named("runKtlintFormatOverKotlinScripts") {
    dependsOn("openApiGenerate")
}

dependencies {
    // -------------------------------------------------
    // Spring Boot starters – runtime dependencies
    // -------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-envers")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.admin.client)
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    // -------------------------------------------------
    // Kotlin & logging
    // -------------------------------------------------
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlin.logging)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // -------------------------------------------------
    // Playwright & HTML parsing
    // -------------------------------------------------
    implementation(libs.playwright)
    implementation(libs.jsoup)

    // -------------------------------------------------
    // Jakarta APIs
    // -------------------------------------------------
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.jakarta.ws.rs.api)
    implementation(libs.jersey.common)
    implementation(libs.springdoc)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.all)
    implementation(libs.resilience4j.kotlin)

    // -------------------------------------------------
    // Database drivers
    // -------------------------------------------------
    runtimeOnly("org.postgresql:postgresql")
    implementation(libs.sqlite.jdbc)

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
    implementation(libs.kotlinx.coroutines.core)
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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.springframework.integration:spring-integration-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-toxiproxy")
    testImplementation(libs.wiremock)
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-integration-test")
    testImplementation(libs.mockk)
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(libs.springmockk)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
