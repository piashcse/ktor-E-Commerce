val ktor_version: String by project
val kotlin_version: String by project
val exposed_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.piashcse"
version = "0.0.1"
application {
     mainClass.set("com.piashcse.ApplicationKt")
     project.setProperty("mainClassName", "com.piashcse.ApplicationKt") // adding this for fatjar

}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
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

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

    // exposed ORM library
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")

    // postgresql
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")
    // date time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    // mail server
    implementation("org.apache.commons:commons-email:1.5")
    // validator
    implementation("org.valiktor:valiktor-core:0.12.0")
    // file extension
    implementation("commons-io:commons-io:2.11.0")
    //swagger
    implementation("io.github.smiley4:ktor-swagger-ui:3.3.1")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.22")

}
tasks.create("stage") {
    dependsOn("installDist")
}