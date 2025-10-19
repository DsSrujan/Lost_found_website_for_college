# 1. Start with a base image that has Java 17 installed.
FROM openjdk:17-jdk-slim

# 2. Set a working directory inside the container.
WORKDIR /app

# 3. Copy the project files into the container.
COPY . .

# 4. Run the Maven command to build the project inside the container.
RUN ./mvnw clean package -DskipTests

# 5. Tell the container what command to run when it starts.
ENTRYPOINT ["java","-jar","/app/target/lost-and-found-0.0.1-SNAPSHOT.jar"]
```

### **Step 2: Add and Push the New File to GitHub**

Now we need to add this new `Dockerfile` to your GitHub repository so that Render can see it.

1.  If your local server is running, stop it (`Ctrl + C`).
2.  In your VS Code terminal, run these commands one by one:

    ```powershell
    # Add the new Dockerfile to Git
    git add Dockerfile
    ```
    ```powershell
    # Commit the change with a clear message
    git commit -m "Add Dockerfile for Render deployment"
    ```
    ```powershell
    # Push the new commit to your GitHub repository
    git push
    
