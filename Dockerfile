# make sure to execute 'mvn clean package' beforehand
FROM openjdk:17.0.1-slim
COPY target/build.jar /app/build.jar
COPY target/lib/* /app/lib/
VOLUME ["/data"]
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/build.jar"]