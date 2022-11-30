FROM eclipse-temurin:19.0.1_10-jdk-ubi9-minimal
ARG JAR_FILE=target/*.jar
ENV SERVER_PORT=8080 \
    BEER_SVC_HOST=http://beer-service:8080
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
