FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="aihub"

WORKDIR /app

COPY target/aihub-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms512m", "-Xmx1024m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]