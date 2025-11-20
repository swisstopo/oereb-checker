package ch.swisstopo.oerebchecker.checks;

import ch.swisstopo.oerebchecker.configs.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.ResponseFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class GetCapabilities extends Check {

    private URL url;
    private boolean canRun = false;


    public GetCapabilities(URI basicUri, GetCapabilitiesConfig config) {

        urlTemplate = "%s/capabilities/%s";
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

    protected void readCapabilities() {

        if (getCapabilitiesTopicCodes.isEmpty()) {
            switch (responseFormat) {
                case xml:
                    if (result.XmlIsValid) {
                        try {
                            Document doc = getResponseXmlDocument();

                            String ePrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/Extract");
                            ePrefix = ePrefix != null ? ePrefix + ":" : "";

                            NodeList topicNodes = doc.getElementsByTagName(ePrefix + "topic");
                            if (topicNodes.getLength() > 0) {

                                String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");
                                eDPrefix = eDPrefix != null ? eDPrefix + ":" : "";

                                String codeTag = eDPrefix + "Code";

                                for (int i = 0; i < topicNodes.getLength(); i++) {
                                    Element topic = (Element) topicNodes.item(i);
                                    String code = topic.getElementsByTagName(codeTag).item(0).getTextContent();
                                    getCapabilitiesTopicCodes.add(code);
                                    logger.debug("getCapabilities - topic code: {}", code);
                                }
                            }

                            NodeList languageNodes = doc.getElementsByTagName(ePrefix + "language");
                            for (int i = 0; i < languageNodes.getLength(); i++) {
                                String language = languageNodes.item(i).getTextContent();
                                getCapabilitiesLanguages.add(language);
                                logger.debug("getCapabilities - language: {}", language);
                            }

                        } catch (Exception e) {
                            logger.error("[{}] - GetCapabilities error: {}", uUId, e.getMessage(), e);
                        }
                    }
                    break;
                case json:

                    break;
                default:
            }
        }
    }

    public CheckResult run() {
        if (canRun) {
            logger.info("[{}] - GetCapabilities start: {}", uUId, url);

            HttpClient client = null;
            try {
                client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                checkResponse(response);

                readCapabilities();

            } catch (Exception e) {
                result.HasError = true;
                result.ErrorMessage = e.getMessage();
                logger.error("[{}] - GetCapabilities error: {}", uUId, e.getMessage(), e);
            } finally {
                if (client != null) {
                    client.shutdownNow();
                    client.close();
                }
            }

            logger.info("[{}] - GetCapabilities done: {}", uUId, url);
            return result;
        }
        return null;
    }
}
