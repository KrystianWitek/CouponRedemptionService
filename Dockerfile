FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup --system spring && adduser --system spring --ingroup spring
COPY --from=builder --chown=spring:spring /workspace/build/libs/application.jar application.jar
USER spring:spring
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java"]
CMD ["-jar", "application.jar"]
