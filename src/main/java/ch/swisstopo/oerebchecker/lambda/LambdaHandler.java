package ch.swisstopo.oerebchecker.lambda;

import ch.swisstopo.oerebchecker.OerebChecker;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger log = context.getLogger();

        // Log startup diagnostics and environment variables to help debug configuration
        // issues in the Lambda environment (where local FS access is restricted).
        log.log("=== Lambda Invocation Started ===");
        log.log("Request ID: " + context.getAwsRequestId());
        log.log("Function Name: " + context.getFunctionName());
        log.log("Memory Limit (MB): " + context.getMemoryLimitInMB());
        log.log("Remaining Time (ms): " + context.getRemainingTimeInMillis());
        log.log("Input: " + input);

        // Log environment for debugging
        log.log("Environment - SCRIPTS_BUCKET: " + System.getenv("SCRIPTS_BUCKET"));
        log.log("Environment - RESULTS_BUCKET: " + System.getenv("RESULTS_BUCKET"));
        log.log("Environment - S3ResultOutputPath: " + System.getenv("S3ResultOutputPath"));

        try {
            // Convert JSON map to CLI args, e.g., {"canton": "AG"} -> ["-canton", "AG"]
            List<String> argList = new ArrayList<>();
            input.forEach((key, value) -> {
                argList.add("-" + key);
                argList.add(String.valueOf(value));
            });

            log.log("Executing OerebChecker with args: " + String.join(" ", argList));

            OerebChecker.execute(argList.toArray(new String[0]));

            log.log("=== Lambda Invocation Completed Successfully ===");
            return "Check completed successfully";

        } catch (Exception e) {
            log.log("=== Lambda Invocation FAILED ===");
            log.log("Exception Type: " + e.getClass().getName());
            log.log("Exception Message: " + e.getMessage());

            // Log full stack trace
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.log("Stack Trace:\n" + sw.toString());

            // Log cause if present
            if (e.getCause() != null) {
                log.log("Root Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                StringWriter causeSw = new StringWriter();
                e.getCause().printStackTrace(new PrintWriter(causeSw));
                log.log("Cause Stack Trace:\n" + causeSw.toString());
            }

            throw new RuntimeException(e);
        }
    }
}
