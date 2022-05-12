#
# Build stage
#
FROM maven:3.8.5-jdk-11-slim AS build
WORKDIR /home/app/
COPY ./ ./
# todo call less heavy target to assembly only spring boot jars
RUN mvn clean install -DskipTests


# Package stage

FROM openjdk:11-jre-slim

WORKDIR /usr/local/lib/app

ENV VERSION=3.3.4

COPY --from=build /home/app/application/target/thingsboard-${VERSION}-boot.jar ./thingsboard-${VERSION}-boot.jar
COPY --from=build /home/app/application/target/data ./data
COPY --from=build /home/app/application/target/conf ./conf

EXPOSE 8080
ENTRYPOINT ["java","-jar","thingsboard-${VERSION}-boot.jar"]