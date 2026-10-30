FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /workspace/target/demo.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EXPOSE 8080
