package ch.swisstopo.oerebchecker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        logger.info("Received event: {}", event);

        // input is the item from the S3 ListObjectsV2 output
        // "Key" contains the object key (path to config file)
        String s3Key = (String) event.get("Key");
        if (s3Key == null) {
            logger.error("No 'Key' found in event");
            throw new RuntimeException("Input event must contain 'Key'");
        }

        String scriptsBucket = System.getenv("SCRIPTS_BUCKET");
        if (scriptsBucket == null || scriptsBucket.isBlank()) {
            // Fallback or error? OerebChecker used "S3Bucket"
            scriptsBucket = System.getenv("S3Bucket");
        }

        if (scriptsBucket == null || scriptsBucket.isBlank()) {
            logger.error("Environment variable SCRIPTS_BUCKET is not set");
            throw new RuntimeException("Environment variable SCRIPTS_BUCKET is not set");
        }

        String resultsBucket = System.getenv("RESULTS_BUCKET");
        if (resultsBucket == null || resultsBucket.isBlank()) {
            // Fallback to scripts bucket if not set (legacy behavior assumption)
            logger.warn("Environment variable RESULTS_BUCKET is not set, defaulting to scripts bucket");
            resultsBucket = scriptsBucket;
        }

        String s3Region = System.getenv("AWS_REGION"); // Standard Lambda Env Var

        logger.info("Processing Config: {} | Input Bucket: {} | Output Bucket: {}", s3Key, scriptsBucket,
                resultsBucket);

        try {
            OerebChecker.runSingleCheck(scriptsBucket, resultsBucket, s3Key, s3Region);
            return "Success: " + s3Key;
        } catch (Exception e) {
            logger.error("Error processing check", e);
            throw new RuntimeException(e);
        }
    }
}
