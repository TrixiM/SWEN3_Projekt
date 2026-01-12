FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# copy parent pom
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Create folders for XML input and archive
RUN mkdir -p /data/access-logs/archive

# Volume for input files
VOLUME /data/access-logs

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
