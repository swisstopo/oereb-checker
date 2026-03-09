package ch.swisstopo.oerebchecker.lambda;

import ch.swisstopo.oerebchecker.OerebChecker;
import ch.swisstopo.oerebchecker.utils.EnvVars;
import ch.swisstopo.oerebchecker.manager.ResultManager;
import ch.swisstopo.oerebchecker.storage.S3StorageProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();

        // Log startup diagnostics and environment variables to help debug configuration
        // issues in the Lambda environment (where local FS access is restricted).
        logger.log("=== Lambda Invocation Started ===");
        logger.log("Request ID: " + context.getAwsRequestId());
        logger.log("Function Name: " + context.getFunctionName());
        logger.log("Memory Limit (MB): " + context.getMemoryLimitInMB());
        logger.log("Remaining Time (ms): " + context.getRemainingTimeInMillis());
        logger.log("Input: " + input);

        // Log environment for debugging
        logger.log("Environment - " + EnvVars.S3_SCRIPTS_BUCKET + ": " + System.getenv(EnvVars.S3_SCRIPTS_BUCKET));
        logger.log("Environment - " + EnvVars.S3_RESULTS_BUCKET + ": " + System.getenv(EnvVars.S3_RESULTS_BUCKET));
        logger.log("Environment - " + EnvVars.S3_RESULTS_OUTPUT_PATH + ": " + System.getenv(EnvVars.S3_RESULTS_OUTPUT_PATH));

        // Aggregate mode: build result.html from all per-canton JSONs written by the
        // parallel canton lambdas. Invoked as the final Step Function step.
        if ("aggregate".equals(input.get("action"))) {
            return runAggregation(context);
        }

        try {
            // Convert JSON map to CLI args, e.g., {"canton": "AG"} -> ["-canton", "AG"]
            List<String> argList = new ArrayList<>();
            input.forEach((key, value) -> {
                argList.add("-" + key);
                argList.add(String.valueOf(value));
            });

            logger.log("Executing OerebChecker with args: " + String.join(" ", argList));

            OerebChecker.execute(argList.toArray(new String[0]));

            logger.log("=== Lambda Invocation Completed Successfully ===");
            return "Check completed successfully";

        } catch (Exception e) {
            logger.log("=== Lambda Invocation FAILED ===");
            logger.log("Exception Type: " + e.getClass().getName());
            logger.log("Exception Message: " + e.getMessage());

            // Log full stack trace
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.log("Stack Trace:\n" + sw.toString());

            // Log cause if present
            if (e.getCause() != null) {
                logger.log("Root Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                StringWriter causeSw = new StringWriter();
                e.getCause().printStackTrace(new PrintWriter(causeSw));
                logger.log("Cause Stack Trace:\n" + causeSw.toString());
            }

            throw new RuntimeException(e);
        }
    }

    private String runAggregation(Context context) {
        context.getLogger().log("=== Aggregate mode: building result.html from canton JSONs ===");
        try {

            S3StorageProvider storage = S3StorageProvider.createFromEnv(EnvVars.S3_RESULTS_BUCKET)
                    .orElseThrow(() -> new RuntimeException("RESULTS_BUCKET not configured"));

            String outputPath = System.getenv(EnvVars.S3_RESULTS_OUTPUT_PATH);
            ResultManager.aggregate(storage, outputPath != null ? Paths.get(outputPath) : Paths.get(""));

            return "Aggregation completed successfully";
        } catch (Exception e) {
            context.getLogger().log("Aggregation failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
