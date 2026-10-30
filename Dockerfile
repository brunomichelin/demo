FROM openjdk:11.0.11-9-jdk

ADD target/demo.war app.war

ENTRYPOINT ["java", "-jar", "/app.war"]

EXPOSE 8080
