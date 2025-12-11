FROM public.ecr.aws/lambda/java:21

LABEL authors="Geocloud AG"
LABEL version="1.0"

# Copy the fat jar to the task root
# The Lambda runtime adds all jars in ${LAMBDA_TASK_ROOT}/lib to the classpath
COPY ./target/oereb-checker-1.0-jar-with-dependencies.jar ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler
CMD ["ch.swisstopo.oerebchecker.LambdaHandler::handleRequest"]