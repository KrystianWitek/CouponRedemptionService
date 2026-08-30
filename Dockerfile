FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup --system spring && adduser --system spring --ingroup spring
COPY --chown=spring:spring build/libs/application.jar application.jar
USER spring:spring
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java"]
CMD ["-jar", "application.jar"]
