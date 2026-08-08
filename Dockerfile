FROM gradle:8.14.5-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon clean bootJar -x test
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/ai-assisted-url-shortener-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
