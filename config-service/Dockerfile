FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar config-service.jar
ENTRYPOINT ["java", "-jar", "config-service.jar"]
