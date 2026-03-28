FROM registry.cn-hangzhou.aliyuncs.com/acs-public/openjdk:21-jdk-alpine

ADD target/Helix-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 16660
ENTRYPOINT java -jar app.jar
