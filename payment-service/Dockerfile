FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar payment-service.jar
ENTRYPOINT ["java", "-jar", "payment-service.jar"]
