FROM eclipse-temurin:21-jdk
ADD target/Helix-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 16660
ENTRYPOINT java -jar app.jar
