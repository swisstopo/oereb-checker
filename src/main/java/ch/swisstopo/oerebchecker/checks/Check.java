package ch.swisstopo.oerebchecker.checks;

import ch.swisstopo.oerebchecker.configs.CheckConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.ResponseStatusCode;
import ch.swisstopo.oerebchecker.utils.ResponseFormat;
import ch.swisstopo.oerebchecker.utils.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import software.amazon.awssdk.utils.StringUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.*;

public abstract class Check {
    protected static final Logger logger = LoggerFactory.getLogger(Check.class);

    protected static List<String> getCapabilitiesTopicCodes = new ArrayList<>();
    protected static List<String> getCapabilitiesLanguages = new ArrayList<>();

    protected String uUId = UUID.randomUUID().toString();

    protected String urlTemplate;
    protected List<ResponseFormat> supportedFormats;

    protected final Map<String, Object> requestParams = new LinkedHashMap<>();

    protected ResponseFormat responseFormat = null;
    protected int responseStatusCode = ResponseStatusCode.OK;

    protected ByteArrayOutputStream responseOutputStream;

    protected CheckResult result = null;


    protected boolean validateConfig(CheckConfig config) {

        if (config == null) {
            logger.error("[{}] - Check: no config", uUId);
            return false;
        }

        try {
            responseFormat = ResponseFormat.valueOf(config.FORMAT);
            if (!supportedFormats.contains(responseFormat)) {
                logger.error("[{}] - Check: Unsupported response format '{}'", uUId, config.FORMAT);
                return false;
            }
        } catch (IllegalArgumentException e) {
            logger.error("[{}] - Check: {}", uUId, e.getMessage(), e);
            return false;
        }

        if (config.ExpectedStatusCode != null) {
            responseStatusCode = config.ExpectedStatusCode;
        }

        return true;
    }

    private void setResponseStream(InputStream responseInputStream) {
        try {
            responseOutputStream = new ByteArrayOutputStream();
            responseInputStream.transferTo(responseOutputStream);
        } catch (IOException e) {
            responseOutputStream = null;
            logger.error("[{}] - setResponseStream error: {}", uUId, e.getMessage(), e);
        }
    }

    protected InputStream getResponseStream() {
        if (responseOutputStream == null) {
            return null;
        }
        return new ByteArrayInputStream(responseOutputStream.toByteArray());
    }

    private DocumentBuilderFactory docBuiFac = null;
    private Document document = null;

    protected Document getResponseXmlDocument() {
        if (document == null) {
            InputStream inputStream = getResponseStream();
            if (inputStream == null) {
                return null;
            }
            if (docBuiFac == null) {
                docBuiFac = DocumentBuilderFactory.newInstance();
                docBuiFac.setNamespaceAware(true);
            }
            try {
                document = docBuiFac.newDocumentBuilder().parse(new InputSource(inputStream));
            } catch (IOException | SAXException | ParserConfigurationException e) {
                logger.error("[{}] - getResponseDocument error: {}", uUId, e.getMessage(), e);
            }
        }
        return document;
    }

    private <T> boolean checkResponseBasics(HttpResponse<T> response) {

        result.StatusCode = response.statusCode();
        if (result.StatusCode == responseStatusCode) {
            result.StatusCodeCorrect = true;
        }
        if (result.StatusCode == ResponseStatusCode.SEE_OTHER && responseFormat != ResponseFormat.url) {
            result.StatusCodeCorrect = false;
        }

        var headers = response.headers().map();
        if (headers.containsKey("Content-Type")) {
            result.ContentType = headers.get("Content-Type").getFirst();
            if (StringUtils.isNotBlank(result.ContentType)) {
                if (result.StatusCode == ResponseStatusCode.OK) {
                    switch (responseFormat) {
                        case pdf:
                            result.ContentTypeCorrect = result.ContentType.startsWith("application/pdf");
                            break;
                        case xml:
                            result.ContentTypeCorrect = result.ContentType.startsWith("application/xml");
                            break;
                        case json:
                            result.ContentTypeCorrect = result.ContentType.startsWith("application/json");
                            break;
                        default:
                            result.ContentTypeCorrect = false;
                    }
                } else if (result.StatusCode == ResponseStatusCode.SEE_OTHER) {
                    result.ContentTypeCorrect = result.ContentType.startsWith("text/html");
                }
            }
        } else if (result.StatusCode == ResponseStatusCode.NO_CONTENT) {
            result.ContentTypeCorrect = true; // because no content-type needed
        }

        return result.StatusCode == ResponseStatusCode.OK;
    }

    private void checkResponseContent() {

        InputStream checkInputStream = getResponseStream();
        if (checkInputStream == null) {
            return;
        }

        if (responseFormat == ResponseFormat.xml) {

            var checkResult = Validator.checkXml(checkInputStream);
            checkResult.Messages.forEach(e -> result.addXsdValidationFailure(e));
            result.XmlIsValid = checkResult.IsValid;

        } else if (responseFormat == ResponseFormat.pdf) {

            var checkResult = Validator.checkPdf(checkInputStream);
            checkResult.Messages.forEach(e -> result.addPdfValidationFailure(e));
            result.PdfIsValid = checkResult.IsValid;
        }
    }

    protected void checkResponse(HttpResponse<InputStream> response) {
        if (result != null && response != null) {
            if (checkResponseBasics(response)) {

                setResponseStream(response.body());
                checkResponseContent();
            }
        }
    }

    public abstract CheckResult run();

    protected URL getUrl(URI basicUri) {

        if (basicUri == null) {
            logger.error("[{}] - Check error: no basicUri", uUId);
            return null;
        }

        StringBuilder urlString = new StringBuilder();
        try {
            urlString.append(String.format(urlTemplate, basicUri, responseFormat));
            if (!requestParams.isEmpty()) {
                urlString.append("/?");
                for (var entry : requestParams.entrySet()) {
                    urlString.append(entry.getKey()).append("=").append(entry.getValue())
                            .append("&");
                }
                urlString.deleteCharAt(urlString.length() - 1);
            }
            return new URI(urlString.toString()).toURL();

        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("[{}] - Check error: Malformed URI '{}'", uUId, urlString);
        }

        return null;
    }
}
