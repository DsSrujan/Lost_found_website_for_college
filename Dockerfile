# 1. Start with a base image that has Java 17 installed.
FROM openjdk:17-jdk-slim

# 2. Set a working directory inside the container.
WORKDIR /app

# 3. Copy the project files into the container.
COPY . .

# 4. Make the Maven wrapper executable (This is the fix).
RUN chmod +x ./mvnw

# 5. Run the Maven command to build the project inside the container.
RUN ./mvnw clean package -DskipTests

# 6. Tell the container what command to run when it starts.
ENTRYPOINT ["java","-jar","/app/target/lost-and-found-0.0.1-SNAPSHOT.jar"]

