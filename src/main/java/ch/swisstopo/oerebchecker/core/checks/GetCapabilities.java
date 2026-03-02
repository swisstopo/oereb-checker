package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetCapabilities extends Check {
    protected static final Logger logger = LoggerFactory.getLogger(GetCapabilities.class);

    private CapabilitiesData parsedCapabilities = null;

    public GetCapabilities(URI basicUri, GetCapabilitiesConfig config) {

        urlTemplate = "%s/capabilities/%s";
        supportedFormats = Arrays.asList(
                ResponseFormat.xml,
                ResponseFormat.json
        );

        if (validateConfig(config)) {

            if (config.Provoke500) {
                uri = RequestHelper.buildUri(urlTemplate, basicUri, ResponseFormat.csv, requestParams);
            } else {
                uri = RequestHelper.buildUri(urlTemplate, basicUri, responseFormat, requestParams);
            }

            if (uri != null) {
                canRun = true;
                result.setUrl(uri);
            } else {
                setCannotRunReason(
                    "URI_BUILD_FAILED",
                    "Could not build request URI for GetCapabilities (missing/invalid parameters or base URL)."
                );
            }
        }
    }

    public CapabilitiesData getParsedCapabilities() {
        return parsedCapabilities != null ? parsedCapabilities : CapabilitiesData.empty();
    }

    @Override
    protected void postProcess() {
        readCapabilities();
    }

    protected void readCapabilities() {

        if (result.StatusCode == ResponseStatusCode.OK
                && responseFormat == ResponseFormat.xml
                && result.XmlIsValid != null
                && result.XmlIsValid) {

            try {
                Document doc = getResponseXmlDocument();
                logger.trace("Parsing GetCapabilities XML to extract topic codes and languages.");

                List<String> topicCodes = new ArrayList<>();
                NodeList nodes = xpath.getNodes(doc, "//e:topic/ed:Code");
                for (int i = 0; i < nodes.getLength(); i++) {
                    String code = nodes.item(i).getTextContent();
                    logger.trace("Found Topic Code in Capabilities: {}", code);
                    topicCodes.add(code);
                }

                List<String> languages = new ArrayList<>();
                NodeList langNodes = xpath.getNodes(doc, "//e:language");
                for (int i = 0; i < langNodes.getLength(); i++) {
                    String lang = langNodes.item(i).getTextContent().toLowerCase();
                    logger.trace("Found Language in Capabilities: {}", lang);
                    languages.add(lang);
                }

                parsedCapabilities = new CapabilitiesData(
                        topicCodes.stream().distinct().toList(),
                        languages.stream().distinct().toList()
                );

                logger.trace("Read {} topics and {} languages from capabilities.", parsedCapabilities.topicCodes().size(), parsedCapabilities.languages().size());
            } catch (Exception e) {
                logger.error("XPath failed to read capabilities: {}", e.getMessage());
                parsedCapabilities = CapabilitiesData.empty();

                if (result != null) {
                    result.addMessage("Capabilities Validation",
                        ValidatorMessage.error(
                            "Capabilities Validation",
                            "CAPABILITIES_XPATH_EVALUATION_FAILED",
                            "Failed to parse GetCapabilities response (XPath evaluation failed).",
                            "//e:topic/ed:Code and //e:language",
                            e.getMessage()
                        )
                    );
                }
            }
        } else {
            parsedCapabilities = CapabilitiesData.empty();
        }
    }
}
