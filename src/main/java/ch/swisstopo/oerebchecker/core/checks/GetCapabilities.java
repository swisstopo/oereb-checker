package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.Arrays;

public class GetCapabilities extends Check {
    protected static final Logger logger = LoggerFactory.getLogger(GetCapabilities.class);


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
                result = new CheckResult(uri);
            }
        }
    }

    @Override
    protected void postProcess() {
        readCapabilities();
    }

    protected void readCapabilities() {

        if (getCapabilitiesTopicCodes.isEmpty() && result.StatusCode == ResponseStatusCode.OK  && responseFormat == ResponseFormat.xml && result.XmlIsValid != null && result.XmlIsValid) {
            try {
                Document doc = getResponseXmlDocument();
                logger.trace("Parsing GetCapabilities XML to extract topic codes and languages.");

                NodeList nodes = xpath.getNodes(doc, "//e:topic/ed:Code");
                for (int i = 0; i < nodes.getLength(); i++) {
                    String code = nodes.item(i).getTextContent();
                    logger.trace("Found Topic Code in Capabilities: {}", code);
                    getCapabilitiesTopicCodes.add(code);
                }

                NodeList langNodes = xpath.getNodes(doc, "//e:language");
                for (int i = 0; i < langNodes.getLength(); i++) {
                    String lang = langNodes.item(i).getTextContent();
                    logger.trace("Found Language in Capabilities: {}", lang);
                    getCapabilitiesLanguages.add(lang);
                }

                logger.trace("Read {} topics and {} languages from capabilities.", getCapabilitiesTopicCodes.size(), getCapabilitiesLanguages.size());
            } catch (Exception e) {
                logger.error("XPath failed to read capabilities: {}", e.getMessage());
            } finally {
                capabilitiesLatch.countDown();
            }
        } else {
            capabilitiesLatch.countDown();
        }
    }
}
