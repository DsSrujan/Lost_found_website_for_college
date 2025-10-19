# 1. Start with a base image that has both Java 17 and Maven installed.
FROM maven:3.9-eclipse-temurin-17 AS build

# 2. Set a working directory inside the container.
WORKDIR /app

# 3. Copy the project files into the container.
COPY . .

# 4. Run the Maven command to build the project.
# We use 'mvn' directly since it's included in this base image.
RUN mvn clean package -DskipTests

# 5. Create a smaller, final image that only has Java, not all the Maven build tools.
FROM openjdk:17-jdk-slim
WORKDIR /app

# 6. Copy ONLY the final .jar file from the build stage into the final image.
COPY --from=build /app/target/lost-and-found-0.0.1-SNAPSHOT.jar .

# 7. Tell the container what command to run when it starts.
ENTRYPOINT ["java","-jar","lost-and-found-0.0.1-SNAPSHOT.jar"]

