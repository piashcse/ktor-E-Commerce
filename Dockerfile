# Multi-stage: build fat-jar, run on slim JRE.
FROM gradle:8.14-jdk17 AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml
RUN ./gradlew dependencies --no-daemon || true
COPY src src
COPY config config
RUN ./gradlew installDist --no-daemon -x test -x detekt -x ktlintCheck

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/ktor-ecom ./
EXPOSE 8080
ENV PORT=8080 HOST=0.0.0.0
CMD ["bin/ktor-ecom"]
