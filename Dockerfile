# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean install -DskipTests

# Stage 2: Create a slim final image to run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/lost-and-found-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 10000
CMD ["java", "-jar", "app.jar"]