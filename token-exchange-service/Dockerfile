FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q test package

FROM eclipse-temurin:21-jre
RUN addgroup --system app && adduser --system --ingroup app app
WORKDIR /app
COPY --from=build /app/target/token-exchange-service-0.0.1-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
