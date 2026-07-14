FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar subscription-service.jar
ENTRYPOINT ["java", "-jar", "subscription-service.jar"]
