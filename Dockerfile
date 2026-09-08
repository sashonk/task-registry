# Сборка (mvn package) выполняется локально; Docker только упаковывает готовый jar.
# Перед docker build/compose build: mvn -pl <MODULE> -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

ARG MODULE
COPY ${MODULE}/target/*.jar app.jar

RUN mkdir -p /app/data
ENTRYPOINT ["java", "-jar", "app.jar"]
