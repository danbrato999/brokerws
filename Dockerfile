FROM gradle:jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean shadowJar --no-daemon

FROM openjdk:11.0.6-jre-slim
EXPOSE 9050
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/brokerws.jar
ENV RABBITMQ_URI localhost
WORKDIR /app
ENTRYPOINT ["java", "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory", "-jar", "./brokerws.jar"]
