FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
# copy parent pom
COPY pom.xml .
# copy only this subproject
COPY document-access-batch-service ./document-access-batch-service
#build only this module
RUN mvn -pl document-access-batch-service -am clean package -DskipTests

# Create folders for XML input and archive
RUN mkdir -p /data/access-logs/archive

# Volume for input files
VOLUME /data/access-logs

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/document-access-batch-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
