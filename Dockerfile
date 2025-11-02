FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/monolith-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-jar","/app/app.jar"]

