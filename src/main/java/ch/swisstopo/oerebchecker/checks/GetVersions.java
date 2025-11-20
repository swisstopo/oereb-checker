package ch.swisstopo.oerebchecker.checks;

import ch.swisstopo.oerebchecker.configs.GetVersionsConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.ResponseFormat;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class GetVersions extends Check {

    private URL url;
    private boolean canRun = false;


    public GetVersions(URI basicUri, GetVersionsConfig config) {

        urlTemplate = "%s/versions/%s";
        supportedFormats = Arrays.asList(ResponseFormat.xml, ResponseFormat.json);

        if (validateConfig(config)) {
            url = getUrl(basicUri);
            if (url != null) {
                canRun = true;
                result = new CheckResult(url);
            }
        }
    }

    public CheckResult run() {
        if (canRun) {
            logger.info("[{}] - GetVersions start: {}", uUId, url);

            HttpClient client = null;
            try {
                client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                checkResponse(response);

            } catch (Exception e) {
                result.HasError = true;
                result.ErrorMessage = e.getMessage();
                logger.error("[{}] - GetVersions error: {}", uUId, e.getMessage(), e);
            } finally {
                if (client != null) {
                    client.shutdownNow();
                    client.close();
                }
            }

            logger.info("[{}] - GetVersions done: {}", uUId, url);
            return result;
        }
        return null;
    }
}
