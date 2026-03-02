package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetVersionsConfig;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
                result.setUrl(uri);
            } else {
                setCannotRunReason(
                    "URI_BUILD_FAILED",
                    "Could not build request URI for GetVersions (missing/invalid parameters or base URL)."
                );
            }
        }
    }

    private boolean checkSupportedVersion(Document doc) {
        try {
            String expected = "extract-2.0";
            String version = xpath.getString(doc, "//v:supportedVersion[v:version]/v:version");

            boolean ok = expected.equals(version);
            if (!ok && result != null) {
                result.addMessage("Business Logic",
                    ValidatorMessage.error(
                        "Business Logic",
                        "VERSIONS_SUPPORTED_VERSION_UNEXPECTED",
                        "Unsupported version in GetVersions response. Expected '" + expected + "', but found '" + version + "'.",
                        "//v:supportedVersion[v:version]/v:version",
                        null
                    )
                );
            }
            return ok;

        } catch (Exception e) {
            logger.error("XPath error in GetVersions: {}", e.getMessage());
            if (result != null) {
                result.addMessage("Business Logic",
                    ValidatorMessage.error(
                        "Business Logic",
                        "VERSIONS_SUPPORTED_VERSION_XPATH_FAILED",
                        "Failed to read supported version from GetVersions response (XPath evaluation failed).",
                        "//v:supportedVersion[v:version]/v:version",
                        e.getMessage()
                    )
                );
            }
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
