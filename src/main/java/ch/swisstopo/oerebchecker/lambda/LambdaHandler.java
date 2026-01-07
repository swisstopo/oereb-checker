package ch.swisstopo.oerebchecker.lambda;

import ch.swisstopo.oerebchecker.OerebChecker;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {

        context.getLogger().log("Lambda triggered with input: " + input);

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
}
