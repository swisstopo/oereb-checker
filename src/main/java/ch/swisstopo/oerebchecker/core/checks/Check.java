package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.CheckConfig;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.core.validation.Validator;
import ch.swisstopo.oerebchecker.utils.EnvVars;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import ch.swisstopo.oerebchecker.utils.XPathHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public abstract class Check implements ICheck {
    protected static final Logger logger = LoggerFactory.getLogger(Check.class);

    protected static final double maxImageAspectRatioPercentageDifference;

    static {
        String envVal = System.getenv(EnvVars.MAX_IMAGE_ASPECT_RATIO_PERCENTAGE_DIFFERENCE);
        double defaultValue = 10.0; // Fallback value
        double value;
        try {
            value = StringUtils.isNotBlank(envVal) ? Double.parseDouble(envVal) : defaultValue;
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        maxImageAspectRatioPercentageDifference = value;
    }

    protected XPathHelper xpath = new XPathHelper();

    protected String cannotRunReasonCode = null;
    protected String cannotRunReasonMessage = null;

    protected String urlTemplate;
    protected List<ResponseFormat> supportedFormats;

    protected final Map<String, Object> requestParams = new LinkedHashMap<>();

    protected URI uri;
    protected boolean canRun = false;
    protected ResponseFormat responseFormat = null;
    protected int responseStatusCode = ResponseStatusCode.OK;
    protected boolean provoke500 = false;
    protected boolean followOneRedirect = false;

    private Document document = null;

    protected CheckResult result = new CheckResult();

    protected void setCannotRunReason(String code, String message) {
        this.cannotRunReasonCode = code;
        this.cannotRunReasonMessage = message;

        logger.error(message);
    }

    protected boolean validateConfig(CheckConfig config) {

        result.ConfigInfo = (config != null ? config.toString() : null);

        if (config == null || !config.isValid()) {
            setCannotRunReason("CONFIG_INVALID", "Configuration is missing or invalid.");
            return false;
        }

        try {
            responseFormat = ResponseFormat.valueOf(config.FORMAT);
            if (!supportedFormats.contains(responseFormat)) {
                setCannotRunReason("UNSUPPORTED_FORMAT","Unsupported response format: '" + config.FORMAT + "'.");
                return false;
            }
        } catch (IllegalArgumentException e) {
            setCannotRunReason("INVALID_FORMAT", "Invalid format: '" + config.FORMAT + "'.");
            return false;
        }

        if (config.ExpectedStatusCode != null) {
            responseStatusCode = config.ExpectedStatusCode;
        }

        if (config.Provoke500) {
            responseStatusCode = ResponseStatusCode.INTERNAL_SERVER_ERROR;
            provoke500 = true;
        }

        followOneRedirect = config.FollowOneRedirect;

        return true;
    }

    public CheckResult run() {
        if (!canRun) {
            if (cannotRunReasonCode == null && cannotRunReasonMessage == null) {
                if (uri == null) {
                    setCannotRunReason(
                        "URI_BUILD_FAILED",
                        "Check could not be executed because no valid request URI was created (check configuration/parameters)."
                    );
                } else {
                    setCannotRunReason(
                        "CANNOT_RUN",
                        "Check could not be executed (canRun=false)."
                    );
                }
            }

            result.ExecutionStatus = CheckStatus.SKIPPED;
            result.NotExecutedReasonCode = cannotRunReasonCode;
            result.NotExecutedReason = cannotRunReasonMessage;

            String reasonText = StringUtils.isNotBlank(result.NotExecutedReason) ? result.NotExecutedReason : "No reason message was provided.";

            result.addMessage("Execution",
                ValidatorMessage.error(
                    "Execution",
                    "CHECK_SKIPPED",
                    "Check was skipped. Reason: " + reasonText,
                    (uri != null ? uri.toString() : result.Url),
                    result.NotExecutedReasonCode
                )
            );

            logger.trace(
                    "Check skipped: {} ({}); checkConfig={}",
                    result.NotExecutedReason,
                    result.NotExecutedReasonCode,
                    result.ConfigInfo
            );
            return result;
        }

        logger.info("Start: {} (checkConfig={})", uri, result.ConfigInfo);
        try {
            HttpClient client = RequestHelper.getSharedHttpClient();
            HttpRequest request = RequestHelper.createRequest(uri);
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (followOneRedirect && responseStatusCode != ResponseStatusCode.SEE_OTHER && isRedirect(response.statusCode())) {

                URI redirectUri = extractRedirectUri(response);
                if (redirectUri != null) {
                    logger.info("Following one redirect: {} -> {}", uri, redirectUri);

                    result.RedirectFollowed = true;
                    result.setRedirectUrl(redirectUri);

                    try (InputStream ignored = response.body()) {
                        // close the first response body before the second request
                    }

                    HttpRequest redirectRequest = RequestHelper.createRequest(redirectUri);
                    response = client.send(redirectRequest, HttpResponse.BodyHandlers.ofInputStream());
                }
            }

            try (InputStream is = response.body()) {
                processResponse(response, is);
            }
            postProcess();

            result.ExecutionStatus = CheckStatus.EXECUTED;

        } catch (Exception e) {
            if (e instanceof IOException ioe &&
                    ioe.getMessage() != null &&
                    ioe.getMessage().equals("unexpected content length header with 204 response")) {

                result.StatusCode = ResponseStatusCode.NO_CONTENT;
                result.StatusCodeCorrect = responseStatusCode == ResponseStatusCode.NO_CONTENT;
                result.ContentTypeCorrect = null; // because no content-type needed

                result.ExecutionStatus = CheckStatus.EXECUTED;

            } else {
                result.ExecutionStatus = CheckStatus.FAILED;
                result.ExceptionType = e.getClass().getName();
                result.ExceptionMessage = e.getMessage();

                result.HasError = true;
                result.ErrorMessage = e.getMessage();

                result.addMessage("Execution",
                    ValidatorMessage.error(
                        "Execution",
                        "CHECK_EXECUTION_EXCEPTION",
                        "Check execution failed due to an unexpected exception.",
                        (uri != null ? uri.toString() : result.Url),
                        (result.ExceptionType != null ? result.ExceptionType + ": " : "") + result.ExceptionMessage
                    )
                );

                logger.error("Check failed for {}: {}", uri, e.getMessage(), e);
            }
        }

        logger.info("Done:  {} (checkConfig={})", uri, result.ConfigInfo);
        return result;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == ResponseStatusCode.MOVED_PERMANENTLY ||
                statusCode == ResponseStatusCode.FOUND ||
                statusCode == ResponseStatusCode.SEE_OTHER ||
                statusCode == ResponseStatusCode.TEMPORARY_REDIRECT ||
                statusCode == ResponseStatusCode.PERMANENT_REDIRECT;
    }

    private URI extractRedirectUri(HttpResponse<?> response) {
        try {
            String location = response.headers().firstValue("location").orElse(null);
            if (StringUtils.isBlank(location)) {
                logger.warn("Redirect response received without Location header");
                return null;
            }

            return response.uri().resolve(location);
        } catch (Exception e) {
            logger.warn("Failed to resolve redirect Location header", e);
            return null;
        }
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
        } else {
            result.addMessage("Protocol Validation",
                ValidatorMessage.error(
                    "HTTP Protocol",
                    "HTTP_STATUS_UNEXPECTED",
                    "Unexpected HTTP status. Expected " + responseStatusCode + ", but received " + result.StatusCode + ".",
                    result.Url,
                    null
                )
            );
        }

        if (result.StatusCode == ResponseStatusCode.SEE_OTHER && responseFormat != ResponseFormat.url) {
            result.StatusCodeCorrect = false;
            result.addMessage("Protocol Validation",
                ValidatorMessage.error(
                    "HTTP Protocol",
                    "HTTP_REDIRECT_UNEXPECTED",
                    "Unexpected redirect (HTTP 303) for a non-URL format request.",
                    result.Url,
                    null
                )
            );
        }

        var headers = response.headers().map();
        logger.trace("Response Headers: {}", headers);

        if (headers.containsKey("Content-Type")) {
            result.ContentType = headers.get("Content-Type").getFirst();
            if (StringUtils.isNotBlank(result.ContentType)) {

                if (result.StatusCode == ResponseStatusCode.SEE_OTHER) {
                    result.ContentTypeCorrect = null;
                    logger.trace("Content-Type check is skipped to be not to strict for redirect response");

                } else if (provoke500) {
                    result.ContentTypeCorrect = null;
                    logger.trace("Content-Type check is skipped to be not to strict for 'provoke500' response");

                } else {
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
                }

                if (result.ContentTypeCorrect != null && !result.ContentTypeCorrect) {
                    result.addMessage("Protocol Validation",
                            ValidatorMessage.error(
                                    "HTTP Protocol",
                                    "HTTP_CONTENT_TYPE_UNEXPECTED",
                                    "Unexpected Content-Type '" + result.ContentType + "' for requested format '" + responseFormat + "'.",
                                    result.Url,
                                    null
                            )
                    );
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
                validationResult.Messages.forEach(msg -> result.addMessage("XSD Validation", msg));

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
            validationResult.Messages.forEach(msg -> result.addMessage("PDF Validation", msg));
            result.PdfIsValid = validationResult.IsValid;
        }
    }

    protected Document getResponseXmlDocument() {
        return document;
    }

    // Hook for child classes to do extra work after validation (like parsing XML)
    protected void postProcess() {
    }

    @Override
    public String toString() {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                //.setPrettyPrinting()
                .create();

        return gson.toJson(this);
    }
}
