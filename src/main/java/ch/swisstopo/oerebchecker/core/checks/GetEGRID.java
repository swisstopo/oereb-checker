package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetEGRIDConfig;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URI;
import java.util.Arrays;

public class GetEGRID extends Check {
    protected static final Logger logger = LoggerFactory.getLogger(GetEGRID.class);


    public GetEGRID(URI basicUri, GetEGRIDConfig config) {

        urlTemplate = "%s/getegrid/%s";
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
                    "Could not build request URI for GetEGRID (missing/invalid parameters or base URL)."
                );
            }
        }
    }

    protected boolean validateConfig(GetEGRIDConfig config) {

        if (!super.validateConfig(config)) {
            return false;
        }

        if (StringUtils.isNotBlank(config.EN)) {
            requestParams.put("EN", config.EN);
        }
        if (StringUtils.isNotBlank(config.GNSS)) {
            requestParams.put("GNSS", config.GNSS);
        }
        if (StringUtils.isNotBlank(config.IDENTDN)) {
            requestParams.put("IDENTDN", config.IDENTDN);
        }
        if (StringUtils.isNotBlank(config.NUMBER)) {
            requestParams.put("NUMBER", config.NUMBER);
        }
        if (config.POSTALCODE != null) {
            requestParams.put("POSTALCODE", config.POSTALCODE);
        }
        if (StringUtils.isNotBlank(config.LOCALISATION)) {
            requestParams.put("LOCALISATION", config.LOCALISATION);
        }

        if (config.GEOMETRY) {
            requestParams.put("GEOMETRY", true);
        }

        return true;
    }
}
