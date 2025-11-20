package ch.swisstopo.oerebchecker.checks;

import ch.swisstopo.oerebchecker.configs.GetEGRIDConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.ResponseFormat;
import software.amazon.awssdk.utils.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class GetEGRID extends Check {

    private URL url;
    private boolean canRun = false;


    public GetEGRID(URI basicUri, GetEGRIDConfig config) {

        urlTemplate = "%s/getegrid/%s";
        supportedFormats = Arrays.asList(
                ResponseFormat.xml,
                ResponseFormat.json
        );

        if (validateConfig(config)) {
            url = getUrl(basicUri);
            if (url != null) {
                canRun = true;
                result = new CheckResult(url);
            }
        }
    }

    protected boolean validateConfig(GetEGRIDConfig config) {

        if (!super.validateConfig(config)) {
            return false;
        }

        if (config.GEOMETRY) {
            requestParams.put("GEOMETRY", true);
        }

        int variants = 0;
        // request variant C
        if (config.POSTALCODE != null && StringUtils.isNotBlank(config.LOCALISATION) && config.NUMBER != null) {
            requestParams.put("POSTALCODE", config.POSTALCODE);
            requestParams.put("LOCALISATION", config.LOCALISATION);
            requestParams.put("NUMBER", config.NUMBER);
            variants++;
        }
        // request variant B
        if (StringUtils.isNotBlank(config.IDENTDN) && config.NUMBER != null) {
            requestParams.put("IDENTDN", config.IDENTDN);
            requestParams.put("NUMBER", config.NUMBER);
            variants++;
        }
        // request variant A
        if (StringUtils.isNotBlank(config.EN)) {
            requestParams.put("EN", config.EN);
            variants++;
        }
        // request variant D
        if (StringUtils.isNotBlank(config.GNSS)) {
            requestParams.put("GNSS", config.GNSS);
            variants++;
        }

        if (variants != 1) {
            logger.error("[{}] - GetEGRID: invalid request variants count: '{}'", uUId, variants);
        }
        return variants == 1;
    }

    public CheckResult run() {
        if (canRun) {
            logger.info("[{}] - GetEGRID start: {}", uUId, url);

            HttpClient client = null;
            try {
                client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                checkResponse(response);

            } catch (Exception e) {
                result.HasError = true;
                result.ErrorMessage = e.getMessage();
                logger.error("[{}] - GetEGRID error: {}", uUId, e.getMessage(), e);
            } finally {
                if (client != null) {
                    client.shutdownNow();
                    client.close();
                }
            }

            logger.info("[{}] - GetEGRID done: {}", uUId, url);
            return result;
        }
        return null;
    }
}
