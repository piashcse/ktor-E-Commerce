@file:OptIn(OpenApiPreview::class)
import io.ktor.plugin.OpenApiPreview

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

group = "com.piashcse"
version = "1.0.0"
application {
    mainClass.set("com.piashcse.ApplicationKt")
    // adding this for fatjar
    project.setProperty("mainClassName", "com.piashcse.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Ktor core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.host.common)

    // Logging
    implementation(libs.logback.classic)

    // Database & migration
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.kotlin.datetime)

    // Utils
    implementation(libs.bcrypt)
    implementation(libs.commons.email)
    implementation(libs.valiktor.core)
    implementation(libs.commons.io)
    implementation(libs.dotenv.kotlin)

    // Swagger / OpenAPI
    implementation(libs.ktor.swagger.ui)
    implementation(libs.ktor.open.api)

    // Dependency injection (Koin)
    implementation(libs.koin.ktor)
    implementation(libs.koin.core)
    implementation(libs.koin.logger)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}


kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

ktor {
    openApi {
        title = "Ktor E-Commerce API"
        version = "1.0.0"
        summary = "E-Commerce API built with Ktor framework"
        description = "This is a complete E-Commerce API with user authentication, product management, cart functionality, and order processing."
        termsOfService = "https://example.com/terms/"
        contact = "support@example.com"
        license = "MIT"
        // Location of the generated specification (defaults to openapi/generated.json)
        target = project.layout.buildDirectory.file("openapi/generated.json")
    }
}

// Copy the generated OpenAPI spec to resources during build process
tasks.processResources {
    dependsOn("buildOpenApi")
    from(layout.buildDirectory.dir("openapi")) {
        into("openapi")
    }
}

tasks.register("stage") {
    dependsOn("installDist")
}