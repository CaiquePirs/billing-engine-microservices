FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar invoice-service.jar
ENTRYPOINT ["java", "-jar", "invoice-service.jar"]
