# Stage 1: Build
FROM gradle:8.14-jdk21-alpine AS builder

# Accept version as build argument
ARG VERSION=0.0.0-SNAPSHOT

WORKDIR /build

# Copy Gradle configuration files first for better caching
COPY settings.gradle.kts build.gradle.kts gradlew gradlew.bat ./
COPY gradle ./gradle

# Copy all module build files
COPY droiddeploy-core/build.gradle.kts ./droiddeploy-core/
COPY droiddeploy-db/build.gradle.kts ./droiddeploy-db/
COPY droiddeploy-rest/build.gradle.kts ./droiddeploy-rest/
COPY droiddeploy-svc/build.gradle.kts ./droiddeploy-svc/

# Download dependencies (cached layer if deps don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY droiddeploy-core/src ./droiddeploy-core/src
COPY droiddeploy-db/src ./droiddeploy-db/src
COPY droiddeploy-rest/src ./droiddeploy-rest/src
COPY droiddeploy-svc/src ./droiddeploy-svc/src

# Build the application with specified version
RUN gradle :droiddeploy-svc:bootJar -Prevision=${VERSION} --no-daemon

# Verify JAR was created
RUN ls -lh /build/droiddeploy-svc/build/libs/*.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install required utilities for healthcheck and APK metadata extraction
RUN apk add --no-cache \
    curl \
    wget \
    ca-certificates \
    tzdata

# Create non-root user and group
RUN addgroup -g 1001 -S droiddeploy && \
    adduser -u 1001 -S droiddeploy -G droiddeploy

# Create application directories with proper ownership
RUN mkdir -p /var/lib/droiddeploy/apks && \
    chown -R droiddeploy:droiddeploy /var/lib/droiddeploy

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/droiddeploy-svc/build/libs/*.jar app.jar

# Verify JAR is readable
RUN ls -lh /app/app.jar

# Change ownership of application files
RUN chown droiddeploy:droiddeploy /app/app.jar

# Switch to non-root user
USER droiddeploy

# Expose application port
EXPOSE 8080

# Health check configuration
# Uses wget instead of curl for better compatibility
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM configuration optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Metadata labels
LABEL org.opencontainers.image.title="DroidDeploy"
LABEL org.opencontainers.image.description="Android APK distribution service"
LABEL org.opencontainers.image.vendor="pashaoleynik97"
LABEL org.opencontainers.image.source="https://github.com/pashaoleynik97/droid-deploy-svc"
LABEL org.opencontainers.image.licenses="MIT"
