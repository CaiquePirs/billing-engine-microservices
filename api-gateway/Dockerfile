FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar api-gateway.jar
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]