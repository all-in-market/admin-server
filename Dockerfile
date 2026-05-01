FROM eclipse-temurin:21-jre-jammy
LABEL authors="all-in-market"
WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]