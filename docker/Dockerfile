# Image with pre compiled resources

FROM openjdk:11-jre-slim

WORKDIR /usr/local/lib/app


COPY ./application/target/data ./data
COPY ./application/target/conf ./conf
COPY ./application/target/thingsboard-3.3.4-SNAPSHOT-boot.jar ./thingsboard-3.3.4-SNAPSHOT-boot.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","thingsboard-3.3.4-SNAPSHOT-boot.jar"]
