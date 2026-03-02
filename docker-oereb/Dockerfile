FROM amazoncorretto:21-alpine

# Best Practice: Set a working directory to avoid using absolute paths everywhere
WORKDIR /app

# Best Practice: Create a specific group and user to run the application (Security)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Change ownership of the working directory
RUN chown -R appuser:appgroup /app

# JAR must be built with: mvn package -Plambda
COPY ./target/oereb-checker-1.0.jar /app/oereb-checker-1.0.jar

LABEL authors="Geocloud AG"
LABEL maintainer="Geocloud AG <https://www.geocloud.ch>"
LABEL version="1.0"

# Best Practice: Switch to the non-root user
USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-cp", "/app/oereb-checker-1.0.jar", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda"]

# CMD: The default argument passed to the ENTRYPOINT (your specific handler)
# This part can be overridden in the AWS Lambda configuration
CMD ["ch.swisstopo.oerebchecker.lambda.LambdaHandler::handleRequest"]