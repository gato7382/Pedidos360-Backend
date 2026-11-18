# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common common
COPY catalog-service catalog-service
COPY orders-service orders-service
COPY audit-service audit-service
COPY report-service report-service
COPY messaging-admin-service messaging-admin-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE_MODULE
COPY --from=build /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-1.0.0.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
