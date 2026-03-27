FROM openjdk:17-jdk-alpine

ADD target/Chat-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 16660
ENTRYPOINT java -jar app.jar
