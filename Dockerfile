# ============================================================
# Stage 1: build the jar with Maven (JDK + Maven, not shipped)
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Download dependencies first: this layer is cached while pom.xml is unchanged
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ============================================================
# Stage 2: runtime image (JRE only, smaller and safer)
# ============================================================
FROM eclipse-temurin:17-jre AS runtime

# curl is used by the healthcheck; app runs as a non-root user
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --no-create-home appuser

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

USER appuser
EXPOSE 8080

# UTC time zone (same fix as in lab 1) and JVM memory relative to container limit
ENV JAVA_OPTS="-Duser.timezone=UTC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]