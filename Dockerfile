# Multi-stage: build fat-jar, run on slim JRE as a non-root user.
FROM gradle:8.14-jdk17 AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon || true
COPY src src
COPY config config
RUN ./gradlew installDist --no-daemon -x test -x detekt -x ktlintCheck

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --create-home appuser
WORKDIR /app
COPY --from=build /app/build/install/ktor-ecom ./
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
ENV PORT=8080 HOST=0.0.0.0
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/health/live || exit 1
CMD ["bin/ktor-ecom"]
