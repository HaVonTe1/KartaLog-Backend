plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.admin.server)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}

springBoot {
    mainClass.set("io.github.havonte1.kartalog.backend.admin.AdminServerApplicationKt")
}
