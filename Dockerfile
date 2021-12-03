#
# Build stage
#
FROM maven:3.8.4-openjdk-17-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM openjdk:17.0.1-slim
COPY --from=build /home/app/target/gallium.jar /usr/local/lib/gallium.jar
COPY --from=build /home/app/target/lib/*  /usr/local/lib/lib/
VOLUME ["/usr/local/lib/data"]
WORKDIR /usr/local/lib/data
ENTRYPOINT ["java","-jar","/usr/local/lib/gallium.jar"]
