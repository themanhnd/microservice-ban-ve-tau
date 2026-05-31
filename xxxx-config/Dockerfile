FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8888
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]
