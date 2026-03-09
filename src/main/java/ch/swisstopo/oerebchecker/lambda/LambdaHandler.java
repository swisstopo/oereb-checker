package ch.swisstopo.oerebchecker.lambda;

import ch.swisstopo.oerebchecker.OerebChecker;
import ch.swisstopo.oerebchecker.manager.ResultManager;
import ch.swisstopo.oerebchecker.storage.S3StorageProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {

        context.getLogger().log("Lambda triggered with input: " + input);

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

            OerebChecker.execute(argList.toArray(new String[0]));
            return "Check completed successfully";

        } catch (Exception e) {
            context.getLogger().log("Execution failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String runAggregation(Context context) {
        context.getLogger().log("=== Aggregate mode: building result.html from canton JSONs ===");
        try {
            S3StorageProvider storage = S3StorageProvider.createFromEnv("RESULTS_BUCKET")
                    .orElseThrow(() -> new RuntimeException("RESULTS_BUCKET not configured"));

            String outputPath = System.getenv("S3ResultOutputPath");
            ResultManager.aggregate(storage, outputPath != null ? Paths.get(outputPath) : Paths.get(""));

            return "Aggregation completed successfully";
        } catch (Exception e) {
            context.getLogger().log("Aggregation failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
