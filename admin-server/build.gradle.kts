plugins {
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.3.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.codecentric:spring-boot-admin-starter-server:4.0.2")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}

springBoot {
    mainClass.set("io.github.havonte1.kartalog.backend.admin.AdminServerApplicationKt")
}
