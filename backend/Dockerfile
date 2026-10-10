# syntax=docker/dockerfile:1

################################################################################
# Build stage: compiles the application and produces the executable fat jar.
################################################################################
FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /build

# Maven wrapper + pom first: this layer is reused while dependencies don't change.
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
COPY pom.xml pom.xml
RUN ./mvnw -B -ntp dependency:go-offline

# Sources change far more often than the pom, so they come last.
COPY src/ src/
RUN ./mvnw -B -ntp package -DskipTests \
    && find target -maxdepth 1 -name '*.jar' ! -name '*.original' -exec cp {} /build/app.jar \;

################################################################################
# Runtime stage: JRE only, non-root user, traditional "java -jar" launch.
################################################################################
FROM eclipse-temurin:25-jre-jammy AS final

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

WORKDIR /app
COPY --from=build --chown=appuser:appuser /build/app.jar app.jar

USER appuser

# Size the heap from the Fargate task memory limit; override via task definition if needed.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8082

# Exec form: java is PID 1 and receives SIGTERM from Fargate for graceful shutdown.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
