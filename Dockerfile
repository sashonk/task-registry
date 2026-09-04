# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY auth auth
COPY tasks tasks
COPY scheduler scheduler
COPY gateway gateway

ARG MODULE
RUN mvn -pl ${MODULE} -am package -DskipTests -q \
    && JAR="$(ls ${MODULE}/target/*.jar | grep -v original | head -1)" \
    && test -n "$JAR" \
    && cp "$JAR" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
RUN mkdir -p /app/data
ENTRYPOINT ["java", "-jar", "app.jar"]
