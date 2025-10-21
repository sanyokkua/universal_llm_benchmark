FROM amazoncorretto:25.0.0-alpine3.22 AS runtime

# Set TZ and environment defaults (can be overridden in docker-compose)
ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS=""

# Create non-root user and app dirs in a single RUN to minimize layers
RUN addgroup -S spring && \
    adduser -S -G spring spring && \
    mkdir -p /app /app/data && \
    chown -R spring:spring /app && \
    apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime || true

WORKDIR /app

# Copy the built jar (expected to be built by host or CI)
# You may want to adjust the jar name pattern if version changes.
COPY target/benchmark-0.0.1-SNAPSHOT.jar /app/app.jar

# Ensure the jar is readable by the non-root user (read-only is fine)
RUN chown spring:spring /app/app.jar && chmod 440 /app/app.jar

# Switch to non-root user
USER spring

EXPOSE 8080

VOLUME ["/app/data"]

# Healthcheck uses wget (already installed). Use CMD-SHELL to allow shell ops like || exit 1
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/ || exit 1

# Use exec form with sh -c to allow JAVA_OPTS expansion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -XX:MaxRAMPercentage=75.0 -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
