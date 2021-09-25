val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.30"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.ktor:ktor-gson:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

    // jwt token
    implementation( "io.ktor:ktor-auth-jwt:$ktor_version")

    // exposed ORM library
    implementation("org.jetbrains.exposed:exposed-core:0.34.1")
    implementation( "org.jetbrains.exposed:exposed-dao:0.34.1")
    implementation( "org.jetbrains.exposed:exposed-jdbc:0.34.1")

    // postgresql
    implementation( "org.postgresql:postgresql:42.2.23")
    implementation( "com.zaxxer:HikariCP:5.0.0")

    // password hashing
    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")

}