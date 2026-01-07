package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.CheckConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.core.validation.Validator;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.utils.XPathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import software.amazon.awssdk.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Check implements ICheck {
    protected static final Logger logger = LoggerFactory.getLogger(Check.class);
    protected static final ReentrantLock capabilitiesLock = new ReentrantLock();
    protected static final CountDownLatch capabilitiesLatch = new CountDownLatch(1);

    protected XPathHelper xpath = new XPathHelper();

    protected static boolean capabilitiesSyncComplete = false;
    protected static List<String> getCapabilitiesTopicCodes = new CopyOnWriteArrayList<>();
    protected static List<String> getCapabilitiesLanguages = new CopyOnWriteArrayList<>();

    protected String urlTemplate;
    protected List<ResponseFormat> supportedFormats;

    protected final Map<String, Object> requestParams = new LinkedHashMap<>();

    protected URI uri;
    protected boolean canRun = false;
    protected ResponseFormat responseFormat = null;
    protected int responseStatusCode = ResponseStatusCode.OK;

    private Document document = null;

    protected CheckResult result = null;


    protected boolean validateConfig(CheckConfig config) {

        if (config == null || !config.isValid()) {
            logger.error("Configuration is missing or invalid");
            return false;
        }

        try {
            responseFormat = ResponseFormat.valueOf(config.FORMAT);
            if (!supportedFormats.contains(responseFormat)) {
                logger.error("Unsupported response format: '{}'", config.FORMAT);
                return false;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid format: {}", config.FORMAT);
            return false;
        }

        if (config.ExpectedStatusCode != null) {
            responseStatusCode = config.ExpectedStatusCode;
        }

        return true;
    }

    public CheckResult run() {
        if (!canRun) {
            logger.trace("Check cannot run: canRun is false for URI {}", uri);
            return null;
        }
        logger.info("Start: {}", uri);

        try {
            HttpClient client = RequestHelper.getSharedHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream is = response.body()) {
                processResponse(response, is);
            }
            postProcess();

        } catch (Exception e) {
            if (result != null) {
                if (e instanceof IOException ioe && ioe.getMessage() != null && ioe.getMessage().equals("unexpected content length header with 204 response")) {
                    result.StatusCode = ResponseStatusCode.NO_CONTENT;
                    result.StatusCodeCorrect = responseStatusCode == ResponseStatusCode.NO_CONTENT;
                    result.ContentTypeCorrect = null; // because no content-type needed
                } else {
                    result.HasError = true;
                    result.ErrorMessage = e.getMessage();
                    logger.error("Check failed for {}: {}", uri, e.getMessage(), e);
                }
            } else {
                logger.error("Check failed for {}: {}", uri, e.getMessage(), e);
            }
        }

        logger.info("Done:  {}", uri);
        return result;
    }

    private void processResponse(HttpResponse<InputStream> response, InputStream is) {
        // 1. Basic check (Status code, Content-Type)
        if (checkResponseBasics(response)) {
            // 2. Point 2: Pass stream directly to validators
            checkResponseContent(is);
        }
    }

    private <T> boolean checkResponseBasics(HttpResponse<T> response) {

        result.StatusCode = response.statusCode();
        logger.trace("Response HTTP Status: {}", result.StatusCode);

        if (result.StatusCode == responseStatusCode) {
            result.StatusCodeCorrect = true;
        }
        if (result.StatusCode == ResponseStatusCode.SEE_OTHER && responseFormat != ResponseFormat.url) {
            result.StatusCodeCorrect = false;
        }

        var headers = response.headers().map();
        logger.trace("Response Headers: {}", headers);

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

    private void checkResponseContent(InputStream is) {

        if (responseFormat == ResponseFormat.xml) {
            try {
                byte[] xmlData = is.readAllBytes();

                var validationResult = Validator.checkXml(new ByteArrayInputStream(xmlData));
                validationResult.Messages.forEach(result::addXsdValidationFailure);

                if (validationResult.IsValid) {
                    logger.trace("XML is valid");
                    result.XmlIsValid = true;
                    document = xpath.parseDocument(new ByteArrayInputStream(xmlData));
                } else {
                    result.XmlIsValid = false;
                    logger.warn("XML is invalid");
                }
            } catch (Exception e) {
                logger.error("Failed to process XML stream", e);
            }

        } else if (responseFormat == ResponseFormat.pdf) {
            var validationResult = Validator.checkPdf(is);
            validationResult.Messages.forEach(result::addPdfValidationFailure);
            result.PdfIsValid = validationResult.IsValid;
        }
    }

    protected Document getResponseXmlDocument() {
        return document;
    }

    // Hook for child classes to do extra work after validation (like parsing XML)
    protected void postProcess() {
    }
}
