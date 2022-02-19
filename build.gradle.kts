val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
plugins {
    application
    kotlin("jvm") version "1.6.0"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
}
tasks.create("stage") {
    dependsOn("installDist")
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    // authentication
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")

    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-compression:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")

    // ktor client
    implementation ("io.ktor:ktor-client-apache:$ktor_version")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

    // exposed ORM library
    implementation("org.flywaydb:flyway-core:8.4.1")
    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")
    implementation("org.jetbrains.exposed:exposed-java-time:0.37.3")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.37.3")

    // postgresql
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // password hashing
    implementation("at.favre.lib:bcrypt:0.9.0")
    // date time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    // mail server
    implementation("org.apache.commons:commons-email:1.5")

    implementation("com.google.api-client:google-api-client:1.33.0")
    implementation("com.google.oauth-client:google-oauth-client:1.32.1")

    // validator
    implementation("org.valiktor:valiktor-core:0.12.0")

    // file extension
    implementation("commons-io:commons-io:2.11.0")

}