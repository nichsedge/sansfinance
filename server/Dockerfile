# Build stage
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :server:installDist

# Run stage
FROM openjdk:17-slim
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/server/build/install/server /app
ENTRYPOINT ["/app/bin/server"]
