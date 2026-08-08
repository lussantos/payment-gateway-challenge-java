FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /application

COPY --from=build /workspace/build/libs/*.jar application.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "application.jar"]
