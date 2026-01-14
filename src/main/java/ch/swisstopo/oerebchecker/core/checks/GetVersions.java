package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetVersionsConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.Arrays;

public class GetVersions extends Check {
    protected static final Logger logger = LoggerFactory.getLogger(GetVersions.class);


    public GetVersions(URI basicUri, GetVersionsConfig config) {

        urlTemplate = "%s/versions/%s";
        supportedFormats = Arrays.asList(ResponseFormat.xml, ResponseFormat.json);

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

    private boolean checkSupportedVersion(Document doc) {
        try {
            String version = xpath.getString(doc, "//v:supportedVersion[v:version='extract-2.0']/v:version");
            return "extract-2.0".equals(version);
        } catch (Exception e) {
            logger.error("XPath error in GetVersions: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected void postProcess() {
        if (result.StatusCode == ResponseStatusCode.OK && responseFormat == ResponseFormat.xml && result.XmlIsValid != null && result.XmlIsValid) {
            logger.trace("Entering postProcess for XML response.");

            Document doc = getResponseXmlDocument();

            result.SupportedVersionIsValid = checkSupportedVersion(doc);
        }
    }
}
