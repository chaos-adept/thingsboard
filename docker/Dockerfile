# Image with pre compiled resources

FROM openjdk:11-jre-slim

WORKDIR /usr/local/lib/app

ENV VERSION=3.3.4

COPY ./application/target/data ./data
COPY ./application/target/conf ./conf
COPY ./application/target/thingsboard-${VERSION}-SNAPSHOT-boot.jar ./thingsboard-${VERSION}-SNAPSHOT-boot.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","thingsboard-${VERSION}-SNAPSHOT-boot.jar"]
