FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY xxxx-common/pom.xml xxxx-common/pom.xml
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/pom.xml ${SERVICE_NAME}/pom.xml
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am -B
COPY xxxx-common/src xxxx-common/src
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src
RUN mvn package -pl ${SERVICE_NAME} -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_NAME
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
