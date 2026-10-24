# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
# os testes rodam no CI e no `mvn verify` local; aqui só empacota
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine
ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Sao_Paulo" \
    INBOX_DIR=/app/inbox
RUN apk add --no-cache tzdata \
    && addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/inbox && chown -R app:app /app
WORKDIR /app
COPY --from=build /build/target/candidaturas-api.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
