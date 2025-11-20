FROM amazoncorretto:21-alpine

COPY ./target/oereb-checker-1.0-jar-with-dependencies.jar /app/oereb-checker-1.0.jar

LABEL authors="Geocloud AG"
LABEL version="1.0"

CMD ["java", "-jar", "/app/oereb-checker-1.0.jar"]