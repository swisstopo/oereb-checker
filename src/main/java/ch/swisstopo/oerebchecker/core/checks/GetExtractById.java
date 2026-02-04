package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oereb.V20Texte;
import ch.swisstopo.oereb.V20Themen;
import ch.swisstopo.oerebchecker.config.models.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.config.models.GetExtractByIdConfig;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.manager.FederalTopicManager;
import ch.swisstopo.oerebchecker.manager.MetadataManager;
import ch.swisstopo.oerebchecker.models.FederalTopicInformation;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class GetExtractById extends Check {
    protected static final Logger logger = LoggerFactory.getLogger(GetExtractById.class);

    private final URI basicUri;
    private final GetExtractByIdConfig config;

    private final HttpClient client = RequestHelper.getSharedHttpClient();

    private final Map<String, HttpResponse<InputStream>> requestedResponseMap = new HashMap<>();


    public GetExtractById(URI basicUri, GetExtractByIdConfig config) {

        urlTemplate = "%s/extract/%s";
        supportedFormats = Arrays.asList(
                ResponseFormat.pdf,
                ResponseFormat.url,
                ResponseFormat.xml,
                ResponseFormat.json
        );

        this.basicUri = basicUri;
        this.config = config;

        if (validateConfig(this.config)) {

            if (this.config.Provoke500) {
                uri = RequestHelper.buildUri(urlTemplate, this.basicUri, ResponseFormat.csv, requestParams);
            } else {
                uri = RequestHelper.buildUri(urlTemplate, this.basicUri, responseFormat, requestParams);
            }

            if (uri != null) {
                canRun = true;
                result = new CheckResult(uri);
            }
        }
    }

    protected boolean validateConfig(GetExtractByIdConfig config) {

        if (!super.validateConfig(config)) {
            return false;
        }

        if (config.EGRIDS.isEmpty()) {
            logger.trace("Validating config for GetExtractById. Input EGRID");
        } else {
            logger.trace("Validating config for GetExtractById. Input EGRIDS count: {}", config.EGRIDS.size());
        }

        if (config.GEOMETRY != null) {
            requestParams.put("GEOMETRY", config.GEOMETRY);
        }
        if (config.SIGNED) {
            requestParams.put("SIGNED", true);
        }
        if (StringUtils.isNotBlank(config.LANG)) {
            requestParams.put("LANG", config.LANG);
        }
        if (StringUtils.isNotBlank(config.TOPICS)) {
            requestParams.put("TOPICS", config.TOPICS);
        }
        if (config.WITHIMAGES != null) {
            requestParams.put("WITHIMAGES", config.WITHIMAGES);
        }

        if (!config.EGRIDS.isEmpty()) {
            Random random = new Random();
            requestParams.put("EGRID", config.EGRIDS.get(random.nextInt(config.EGRIDS.size() - 1)));

        } else {
            if (StringUtils.isNotBlank(config.IDENTDN)) {
                requestParams.put("IDENTDN", config.IDENTDN);
            }
            if (config.NUMBER != null) {
                requestParams.put("NUMBER", config.NUMBER);
            }
            if (StringUtils.isNotBlank(config.EGRID)) {
                requestParams.put("EGRID", config.EGRID);
            }
        }

        return true;
    }

    private CapabilitiesData getCapabilitiesData() {
        return CapabilitiesRegistry.getOrLoad(basicUri, this::loadCapabilitiesForThisEndpoint);
    }

    private CapabilitiesData loadCapabilitiesForThisEndpoint() {
        try {
            GetCapabilitiesConfig cfg = new GetCapabilitiesConfig();
            cfg.FORMAT = ResponseFormat.xml.name();
            cfg.ExpectedStatusCode = 200;

            GetCapabilities check = new GetCapabilities(basicUri, cfg);
            CheckResult capResult = check.run();

            if (capResult == null) {
                if (result != null) {
                    result.addMessage("Capabilities Validation",
                            ValidatorMessage.error("Capabilities", "Load", "GetCapabilities did not return a result.", "")
                    );
                }
                return CapabilitiesData.empty();
            }

            if (capResult.StatusCode != ResponseStatusCode.OK || capResult.XmlIsValid == null || !capResult.XmlIsValid) {
                if (result != null) {
                    result.addMessage("Capabilities Validation",
                            ValidatorMessage.error("Capabilities", "Load", "GetCapabilities response was not OK or XML was invalid.", "")
                    );
                }
                return CapabilitiesData.empty();
            }

            return check.getParsedCapabilities();

        } catch (Exception e) {
            logger.error("Failed to load capabilities: {}", e.getMessage(), e);
            if (result != null) {
                result.addMessage("Capabilities Validation",
                        ValidatorMessage.error("Capabilities", "Load", "Failed to load or parse GetCapabilities.", e.getMessage())
                );
            }
            return CapabilitiesData.empty();
        }
    }


    private boolean checkRealEstate_DPR(Document doc) {
        try {
            String egrid = xpath.getString(doc, "//ed:RealEstate/ed:EGRID");
            String identDN = xpath.getString(doc, "//ed:RealEstate/ed:IdentDN");
            String number = xpath.getString(doc, "//ed:RealEstate/ed:Number");

            boolean egridMatch = egrid.equals(requestParams.get("EGRID"));
            boolean identMatch = !identDN.isBlank() && !number.isBlank();

            boolean valid = egridMatch || identMatch;
            if (!valid) {
                result.addMessage("Business Logic",
                        ValidatorMessage.error("Business Logic", "RealEstate Match", "XML RealEstate does not match requested EGRID: " + requestParams.get("EGRID"), "")
                );
            }
            return valid;
        } catch (Exception e) {
            logger.error("XPath error in checkRealEstate_DPR: {}", e.getMessage());
            return false;
        }
    }

    private HttpResponse<InputStream> getResponse(String uri) throws Exception {
        HttpRequest request;
        HttpResponse<InputStream> response;

        if (!requestedResponseMap.containsKey(uri)) {
            logger.trace("Cache miss for URI: {}. Initiating network request.", uri);
            request = RequestHelper.createRequest(new URI(uri));
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            requestedResponseMap.put(uri, response);
        } else {
            logger.trace("Cache hit for URI: {}", uri);
        }
        return requestedResponseMap.get(uri);
    }

    private Boolean checkRefs(Document doc) {

        try {
            logger.trace("Starting checkRefs XPath evaluation.");

            // Select all nodes that contain a URI/URL, regardless of tag name
            NodeList uriNodes = xpath.getNodes(doc, "//*[" +
                    "ed:LocalisedUri/ed:Text or " +
                    "self::ed:SymbolRef or " +
                    "self::ed:LogoPLRCadastreRef or " +
                    "self::ed:FederalLogoRef or " +
                    "self::ed:CantonalLogoRef or " +
                    "self::ed:MunicipalityLogoRef or " +
                    "self::ed:QRCodeRef]");

            boolean allValid = true;
            for (int i = 0; i < uriNodes.getLength(); i++) {
                String uri = xpath.getString(uriNodes.item(i), ".//ed:Text | text()");
                if (!uri.isEmpty()) {
                    allValid = checkRef(uri, uriNodes.item(i)) && allValid;
                }
            }
            return allValid;

        } catch (Exception e) {
            logger.error("XPath error in checkRefs: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRef(String uri, Node node) {
        try {
            HttpResponse<InputStream> response = getResponse(uri); // getResponse-fct has a cache
            if (response.statusCode() != 200) {
                String msg = "Failed to load reference '" + uri + "' (HTTP " + response.statusCode() + ")";
                result.addMessage("Reference Check",
                        ValidatorMessage.error("Reference Check", "Broken Link", msg, xpath.getPath(node))
                );
                logger.debug("{}. Location: {}", msg, xpath.getPath(node));
                return false;
            } else {
                logger.debug("Successfully loaded reference '{}'", uri);
            }
        } catch (Exception e) {
            logger.debug("Error checking reference at node {}: {}", xpath.getPath(node), e.getMessage());
            return false;
        }
        return true;
    }

    private boolean checkParamGeometry(Document doc) {
        try {
            int geometryCount = xpath.getCount(doc, "//*[local-name()='Point' or local-name()='Line' or local-name()='Surface']");

            if (config.GEOMETRY != null && config.GEOMETRY) {
                int restrictionCount = xpath.getCount(doc, "//ed:RestrictionOnLandownership");
                if (geometryCount != restrictionCount) {
                    result.addMessage("Parameter Validation",
                            ValidatorMessage.error("Parameter Logic", "GEOMETRY=true", "Expected " + restrictionCount + " (one per restriction), but found " + geometryCount, "")
                    );
                    return false;
                }
            } else if (geometryCount > 0) {
                result.addMessage("Parameter Validation",
                        ValidatorMessage.error("Parameter Logic", "GEOMETRY=false", "Geometries found in XML although GEOMETRY=false was requested.", "")
                );
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean checkParamWithImages(Document doc) {
        try {
            // Count all embedded images (Blobs)
            int imageCount = xpath.getCount(doc, "//ed:Image | " +
                    "//ed:Symbol | " +
                    "//ed:LogoPLRCadastre | " +
                    "//ed:FederalLogo | " +
                    "//ed:CantonalLogo | " +
                    "//ed:MunicipalityLogo | " +
                    "//ed:QRCode");

            // Count all external references (Refs)
            int refCount = xpath.filterBySuffix(xpath.getNodes(doc, "//*"), "Ref").size();

            if (config.WITHIMAGES != null && config.WITHIMAGES) {
                // If WITHIMAGES is true, we expect Blobs but NO Refs
                return refCount == 0 && imageCount > 0;
            } else {
                // If WITHIMAGES is false, we expect Refs but NO Blobs
                return imageCount == 0 && refCount > 0;
            }
        } catch (Exception e) {
            logger.error("XPath error in checkParamWithImages: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkImageAspectRatio(Node imageNode, InputStream inputStream, int height, int width) throws Exception {

        float expectedAspectRatio = ((float) height) / ((float) width);

        logger.trace("Checking aspect ratio at node {}. Expected: {} (H:{} / W:{})", xpath.getPath(imageNode), expectedAspectRatio, height, width);

        // Wrap in BufferedInputStream to support mark/reset
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        inputStream.mark(8); // Mark the position to reset later
        byte[] signature = new byte[8];
        int bytesRead = inputStream.read(signature);
        inputStream.reset(); // Reset stream to the beginning

        boolean isPngContent = bytesRead == 8 &&
                signature[0] == (byte) 0x89 &&
                signature[1] == (byte) 0x50 && // P
                signature[2] == (byte) 0x4E && // N
                signature[3] == (byte) 0x47 && // G
                signature[4] == (byte) 0x0D &&
                signature[5] == (byte) 0x0A &&
                signature[6] == (byte) 0x1A &&
                signature[7] == (byte) 0x0A;

        if (!isPngContent) {
            String title = "Invalid image format";
            String message = "The image format is invalid because it is not a PNG at node: " + xpath.getPath(imageNode);

            result.addMessage("Image Validation", ValidatorMessage.error(title, message));
            logger.debug("{}", message);
        }

        BufferedImage image = ImageIO.read(inputStream);
        float existingAspectRatio = (float) image.getHeight() / (float) image.getWidth();

        logger.trace("Image dimensions: {}x{}. Calculated aspect ratio: {}", image.getWidth(), image.getHeight(), existingAspectRatio);

        boolean isAspectRatioValid = false;

        double percentageDifference = 0.0;
        if (expectedAspectRatio == 0) {
            String title = "Invalid image aspect ratio";
            String message = "The 'expectedAspectRatio' cannot be zero (check requested height/width).";

            result.addMessage("Image Validation", ValidatorMessage.error(title, message));
            logger.debug("{}", message);

        } else {
            // Calculate the percentage difference relative to the expected value
            percentageDifference = Math.abs((existingAspectRatio - expectedAspectRatio) / expectedAspectRatio) * 100;
            isAspectRatioValid = percentageDifference <= maxImageAspectRatioPercentageDifference;
        }

        if (!isAspectRatioValid) {
            String title = "Invalid image aspect ratio";
            String message = "The image aspect ratio is invalid. The percentage difference is " + String.format("%.2f", percentageDifference) + "%. Expected: " + expectedAspectRatio + ", Found: " + existingAspectRatio + " at node: " + xpath.getPath(imageNode);

            result.addMessage("Image Validation", ValidatorMessage.error(title, message));
            logger.debug("{}", message);
        }

        return isPngContent && isAspectRatioValid;
    }

    private Boolean checkImageAspectRatios(Document doc) {
        boolean isValid = true;

        Map<String, Boolean> checkedMap = new HashMap<>();
        if (config.WITHIMAGES != null && config.WITHIMAGES) {
            try {
                List<Node> blobNodes = xpath.getNodesList(doc, "//ed:Blob | //ed:Symbol");

                for (Node node : blobNodes) {
                    String base64 = node.getTextContent();
                    Node parentNode = node.getParentNode();
                    try {
                        String parentLocalName = xpath.getSafeLocalName(parentNode);
                        int h = parentLocalName.equals("LocalisedBlob") ? 99 : 3;
                        int w = parentLocalName.equals("LocalisedBlob") ? 174 : 6;

                        boolean nodeIsValid = validateAndCacheImage(base64, parentNode, h, w, checkedMap);
                        if (!nodeIsValid) {
                            result.addMessage("Image Validation",
                                    ValidatorMessage.error("Invalid image", "The image aspect ratio is invalid or it is not a PNG at node: " + xpath.getPath(parentNode))
                            );
                        }
                        isValid = nodeIsValid && isValid;
                    } catch (Exception e) {
                        logger.error("Error checking image aspect ratios at node {}: {}", xpath.getPath(parentNode), e.getMessage());
                        isValid = false;
                    }
                }
            } catch (Exception e) {
                logger.error("XPath error in checkImageAspectRatios: {}", e.getMessage());
                return false;
            }
        } else {
            try {
                // 1. Check ReferenceWMS (MultilingualUri)
                List<Node> wmsNodes = xpath.getNodesList(doc, "//ed:ReferenceWMS//ed:LocalisedUri");
                for (Node localisedUri : wmsNodes) {
                    try {
                        String uri = xpath.getString(localisedUri, "ed:Text");
                        isValid = validateAndCacheRemoteImage(uri, localisedUri, 99, 174, checkedMap) && isValid;
                    } catch (Exception e) {
                        logger.error("Error checking image aspect ratios at node {}: {}", xpath.getPath(localisedUri.getParentNode()), e.getMessage());
                        isValid = false;
                    }
                }

                // 2. Check SymbolRef (anyURI)
                List<Node> symbolRefNodes = xpath.getNodesList(doc, "//ed:SymbolRef");
                for (Node symbolRef : symbolRefNodes) {
                    try {
                        String uri = symbolRef.getTextContent();
                        isValid = validateAndCacheRemoteImage(uri, symbolRef, 3, 6, checkedMap) && isValid;
                    } catch (Exception e) {
                        logger.error("Error checking image aspect ratios at node {}: {}", xpath.getPath(symbolRef.getParentNode()), e.getMessage());
                        isValid = false;
                    }
                }
            } catch (Exception e) {
                logger.error("XPath error in checkImageAspectRatios: {}", e.getMessage());
                return false;
            }
        }
        return isValid;
    }

    private boolean validateAndCacheImage(String base64, Node node, int h, int w, Map<String, Boolean> cache) throws Exception {
        if (!cache.containsKey(base64)) {
            byte[] decodedBytes = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
            InputStream is = new ByteArrayInputStream(decodedBytes);
            cache.put(base64, checkImageAspectRatio(node, is, h, w));
        }
        return cache.get(base64);
    }

    private boolean validateAndCacheRemoteImage(String uri, Node node, int h, int w, Map<String, Boolean> cache) throws Exception {
        if (StringUtils.isBlank(uri)) {
            result.addMessage("Image Validation",
                    ValidatorMessage.error("Broken Reference", "Empty URI", "An image reference (WMS/Symbol) has an empty or blank URI.", xpath.getPath(node))
            );
            return false;
        }
        if (!cache.containsKey(uri)) {
            HttpResponse<InputStream> response = getResponse(uri);
            if (response.statusCode() == 200) {
                try (InputStream is = response.body()) {
                    cache.put(uri, checkImageAspectRatio(node, is, h, w));
                }
            } else {
                cache.put(uri, false);
                String msg = "Failed to load image for aspect ratio check: " + uri + " (HTTP " + response.statusCode() + ")";
                result.addMessage("Image Validation",
                        ValidatorMessage.error("Network Error", "Image Load", msg, xpath.getPath(node))
                );
                logger.warn(msg);
            }
        }
        return cache.get(uri);
    }

    private boolean checkParamLang(Document doc) {

        CapabilitiesData caps = getCapabilitiesData();
        List<String> capabilitiesLanguages = new ArrayList<>(caps.languages());

        if (capabilitiesLanguages.isEmpty()) {
            String msg = "Language check failed: No languages found in GetCapabilities response";
            result.addMessage("Language Validation",
                    ValidatorMessage.error("Capabilities", "Language Sync", msg, "")
            );
            logger.error(msg);
            return false;
        }

        try {
            List<String> foundLanguages = new ArrayList<>(xpath.getNodesList(doc, "//*[local-name()='Language']")
                    .stream()
                    .map(Node::getTextContent)
                    .map(String::toLowerCase)
                    .distinct()
                    .toList());

            if (foundLanguages.isEmpty()) {
                result.addMessage("Language Validation",
                        ValidatorMessage.error("Parameter Logic", "LANG=null", "No language found in XML", "")
                );
                return false;
            }

            if (StringUtils.isNotBlank(config.LANG)) {
                boolean contains = foundLanguages.contains(config.LANG) && foundLanguages.size() == 1;
                if (!contains) {
                    result.addMessage("Language Validation",
                            ValidatorMessage.error("Parameter Logic", "LANG=" + config.LANG, "Requested language '" + config.LANG + "' not found or multiple languages present: " + foundLanguages, "")
                    );
                }
                return contains;

            } else {
                foundLanguages.removeAll(capabilitiesLanguages);
                boolean valid = foundLanguages.isEmpty();
                if (!valid) {
                    result.addMessage("Language Validation",
                            ValidatorMessage.error("Parameter Logic", "LANG not set", "Included language/s not found in capabilities: " + foundLanguages, "")
                    );
                }
                return valid;
            }
        } catch (Exception e) {
            logger.error("XPath error in checkParamLang: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkTopics(Document doc) {
        try {
            NodeList concernedThemeNodes = xpath.getNodes(doc, "//ed:ConcernedTheme");
            NodeList notConcernedThemeNodes = xpath.getNodes(doc, "//ed:NotConcernedTheme");
            NodeList themeWithoutDataNodes = xpath.getNodes(doc, "//ed:ThemeWithoutData");

            if (concernedThemeNodes.getLength() == 0 && notConcernedThemeNodes.getLength() == 0 && themeWithoutDataNodes.getLength() == 0) {
                result.addMessage("Topic Validation",
                        ValidatorMessage.error("No topics available", "There are no entries for <ConcernedTheme>, <NotConcernedTheme>, or <ThemeWithoutData>.")
                );
                return false;
            }

            List<String> extractThemeCodes = new ArrayList<>();

            boolean titlesAreValid = true;
            boolean federalTopicLawDocumentsAreValid = true;

            for (int i = 0; i < concernedThemeNodes.getLength(); i++) {
                Element theme = (Element) concernedThemeNodes.item(i);
                String code = xpath.getString(theme, "ed:Code");
                extractThemeCodes.add(code);

                titlesAreValid = checkTopicTitle(code, theme) && titlesAreValid;
                federalTopicLawDocumentsAreValid = checkFederalTopicLawDocuments(doc, code) && federalTopicLawDocumentsAreValid;
            }
            for (int i = 0; i < notConcernedThemeNodes.getLength(); i++) {
                Element theme = (Element) notConcernedThemeNodes.item(i);
                String code = xpath.getString(theme, "ed:Code");
                extractThemeCodes.add(code);

                titlesAreValid = checkTopicTitle(code, theme) && titlesAreValid;
            }
            for (int i = 0; i < themeWithoutDataNodes.getLength(); i++) {
                Element theme = (Element) themeWithoutDataNodes.item(i);
                String code = xpath.getString(theme, "ed:Code");
                extractThemeCodes.add(code);

                titlesAreValid = checkTopicTitle(code, theme) && titlesAreValid;
            }

            result.FederalTopicLawDocumentsIsValid = federalTopicLawDocumentsAreValid;

            return titlesAreValid && checkTopicCodesAgainstCapabilities(extractThemeCodes);

        } catch (Exception e) {
            logger.error("Error checking topics: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkTopicTitle(String code, Element theme) {

        try {
            FederalTopicInformation federalTopicInformation = MetadataManager.getFederalTopicInformation().get(code);
            if (federalTopicInformation == null) {
                return true;
            }

            boolean titleIsValid = true;

            V20Themen.DATASECTION.V20Thema.V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText federalLocalisedText;

            NodeList themeTexts = xpath.getNodes(theme, ".//ed:LocalisedText");
            for (int i = 0; i < themeTexts.getLength(); i++) {
                try {
                    Node node = themeTexts.item(i);
                    String lang = xpath.getString(node, "ed:Language");
                    String text = xpath.getString(node, "ed:Text");

                    federalLocalisedText = federalTopicInformation.getThema().getTitel().getLocalisationCHV1MultilingualText()
                            .getLocalisedText()
                            .getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equalsIgnoreCase(lang))
                            .findFirst().orElse(null);

                    if (federalLocalisedText != null) {
                        String expectedRaw = federalLocalisedText.getText();
                        String expectedParens = extractParenthesizedParts(expectedRaw);
                        String expectedClean = removeParenthesizedParts(expectedRaw).trim();

                        if (expectedClean != null && text != null) {
                            if (text.equals(expectedClean)) {
                                // OK (matches after cleaning expected)
                                // If we removed something from expected, mention it as WARNING (informational)
                                if (expectedParens != null) {
                                    String msg = "Theme '" + code + "': Title matched after removing parenthesized parts from federal template for language '"
                                            + lang + "'. Removed from expected: " + expectedParens
                                            + ". Expected(raw): '" + expectedRaw + "', Expected(clean): '" + expectedClean + "', Found: '" + text + "'";
                                    result.addMessage("Topic Validation",
                                            ValidatorMessage.warning("Topic Title", "Expected had parenthesized parts", msg, null)
                                    );
                                }
                            } else if (text.contains(expectedClean)) {
                                // Extract contains expected (clean) but has additions => WARNING
                                String msg = "Theme '" + code + "': Title contains federal template text but has additions for language '"
                                        + lang + "'. Expected(clean): '" + expectedClean + "', Found: '" + text + "'"
                                        + (expectedParens != null ? (", Removed from expected: " + expectedParens + " (raw expected: '" + expectedRaw + "')") : "");
                                result.addMessage("Topic Validation",
                                        ValidatorMessage.warning("Topic Title", "Title text extended", msg, null)
                                );
                            } else {
                                // Not contained => ERROR (after cleaning expected)
                                titleIsValid = false;
                                String msg = "Theme '" + code + "': Title does not contain expected federal template text for language '"
                                        + lang + "'. Expected(clean): '" + expectedClean + "', Found: '" + text + "'"
                                        + (expectedParens != null ? (", Removed from expected: " + expectedParens + " (raw expected: '" + expectedRaw + "')") : "");
                                result.addMessage("Topic Validation",
                                        ValidatorMessage.error("Incorrect theme title", msg)
                                );
                                logger.debug(msg);
                            }
                        } else {
                            titleIsValid = false;
                            String msg = "Theme '" + code + "': Missing/empty title text for language '" + lang + "'";
                            result.addMessage("Topic Validation",
                                    ValidatorMessage.error("Incorrect theme title", msg)
                            );
                            logger.debug(msg);
                        }
                    }
                } catch (Exception e) {
                    titleIsValid = false;
                    logger.error("Failed to check topic title: {}", e.getMessage());
                    logger.error("Failed path: {}", xpath.getPath(themeTexts.item(i)));
                    logger.error("Failed node: {}", themeTexts.item(i));
                }
            }
            return titleIsValid;
        } catch (Exception ex) {
            logger.error("XPath error in checkTopicTitle: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Removes parenthesized parts like "Nutzungsplanung (kantonal/kommunal)" -> "Nutzungsplanung".
     * Used only for comparison (equals/contains).
     */
    private static String removeParenthesizedParts(String text) {
        if (text == null) return null;
        String cleaned = text.replaceAll("\\s*\\([^)]*\\)", "");
        return cleaned.trim().replaceAll("\\s{2,}", " ");
    }

    /**
     * Extracts all "(...)" parts for reporting, returns null if none.
     */
    private static String extractParenthesizedParts(String text) {
        if (text == null) return null;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\([^)]*\\)").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(m.group());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private boolean checkTopicCodesAgainstCapabilities(List<String> extractThemeCodes) {
        try {
            logger.trace("Comparing extract theme codes ({}) against capabilities topic codes.", extractThemeCodes);

            CapabilitiesData caps = getCapabilitiesData();
            List<String> capabilitiesTopicCodes = new ArrayList<>(caps.topicCodes());

            if (capabilitiesTopicCodes.isEmpty()) {
                String msg = "Theme check failed: No topics found in GetCapabilities response";
                result.addMessage("Topic Validation",
                        ValidatorMessage.error("Capabilities", "Topic Sync", msg, "")
                );
                logger.error(msg);
                return false;
            }

            List<String> themeNotInGetCapabilities = new ArrayList<>();

            for (String extractThemeCode : extractThemeCodes) {
                if (!capabilitiesTopicCodes.contains(extractThemeCode)) {
                    themeNotInGetCapabilities.add(extractThemeCode);
                    result.addMessage("Topic Validation",
                            ValidatorMessage.error("Theme missing from capabilities", "Extract theme '" + extractThemeCode + "' is not present in GetCapabilities response")
                    );
                }
            }

            if (StringUtils.isBlank(config.TOPICS) || config.TOPICS.equalsIgnoreCase("ALL")) {

                List<String> themeNotInExtract = new ArrayList<>();

                for (String themeCode : capabilitiesTopicCodes) {
                    if (!extractThemeCodes.contains(themeCode)) {
                        themeNotInExtract.add(themeCode);
                        result.addMessage("Topic Validation",
                                ValidatorMessage.error("Capabilities theme missing in extract", "Capabilities theme '" + themeCode + "' is not present in extract")
                        );
                    }
                }

                return themeNotInGetCapabilities.isEmpty() && themeNotInExtract.isEmpty();

            } else if (config.TOPICS.equalsIgnoreCase("ALL_FEDERAL")) {

                List<String> federalTopicCodes = new ArrayList<>(MetadataManager.getFederalTopicInformation().keySet());
                List<String> themeNotInFederalTopics = new ArrayList<>();
                List<String> federalThemeNotInExtract = new ArrayList<>();

                for (String extractThemeCode : extractThemeCodes) {
                    if (!federalTopicCodes.contains(extractThemeCode)) {
                        themeNotInFederalTopics.add(extractThemeCode);
                        result.addMessage("Topic Validation",
                                ValidatorMessage.error("Theme missing from federal topics", "Extract theme '" + extractThemeCode + "' is not present in federal topics")
                        );
                    }
                }
                for (String themeCode : federalTopicCodes) {
                    if (!extractThemeCodes.contains(themeCode)) {
                        federalThemeNotInExtract.add(themeCode);
                        result.addMessage("Topic Validation",
                                ValidatorMessage.error("Federal theme missing in extract", "Federal theme '" + themeCode + "' is not present in extract")
                        );
                    }
                }

                return themeNotInFederalTopics.isEmpty() && federalThemeNotInExtract.isEmpty();

            } else {

                List<String> requestedThemeCodes = Arrays.asList(config.TOPICS.split(","));

                List<String> missingRequestedThemeCodes = new ArrayList<>(requestedThemeCodes);
                missingRequestedThemeCodes.removeAll(extractThemeCodes);
                for (String themeCode : missingRequestedThemeCodes) {
                    result.addMessage("Topic Validation",
                            ValidatorMessage.error("Requested theme missing in extract", "Requested theme '" + themeCode + "' is not present in extract")
                    );
                }

                List<String> additionalExtractThemeCodes = new ArrayList<>(extractThemeCodes);
                additionalExtractThemeCodes.removeAll(requestedThemeCodes);
                for (String themeCode : additionalExtractThemeCodes) {
                    result.addMessage("Topic Validation",
                            ValidatorMessage.error("Unrequested theme in extract", "Theme '" + themeCode + "' is not requested but present in extract")
                    );
                }

                return missingRequestedThemeCodes.isEmpty() && additionalExtractThemeCodes.isEmpty();
            }

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }

        return false;
    }

    private boolean checkFederalTopicLawDocuments(Document doc, String topicCode) {

        if (!MetadataManager.getFederalTopicInformation().containsKey(topicCode)) {
            return true;
        }

        if (MetadataManager.getFederalTopicInformation().get(topicCode).getDokumente().isEmpty()) {
            return true;
        }

        try {
            String query = String.format("//ed:RestrictionOnLandownership[ed:Theme/ed:Code='%s']/ed:LegalProvisions", topicCode);
            NodeList legalProvisionsNodes = xpath.getNodes(doc, query);

            if (legalProvisionsNodes.getLength() == 0) {
                String msg = "Topic '" + topicCode + "' has no <LegalProvisions> but federal metadata requires them.";
                result.addMessage("Topic Validation",
                        ValidatorMessage.error("Federal Logic", "LegalProvisions", msg, "")
                );
                return false;
            }

            String restrictionQuery = String.format("//ed:RestrictionOnLandownership[ed:Theme/ed:Code='%s']", topicCode);
            Node restriction = xpath.getNode(doc, restrictionQuery);

            List<ValidatorMessage> errors = FederalTopicManager.checkFederalTopicInformation(
                    MetadataManager.getFederalTopicInformation().get(topicCode).getDokumente().stream().toList(),
                    legalProvisionsNodes,
                    xpath.getPath(restriction)
            );
            errors.forEach(msg -> result.addMessage("Topic Validation", msg));
            return errors.isEmpty();

        } catch (Exception e) {
            logger.error("XPath error in checkFederalTopicLawDocuments: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRestrictionOnLandownership(Document doc) {
        try {
            boolean isValid = true;
            NodeList restrictions = xpath.getNodes(doc, "//ed:RestrictionOnLandownership");

            for (int i = 0; i < restrictions.getLength(); i++) {
                Node restriction = restrictions.item(i);
                int shareEntries = xpath.getCount(restriction, "ed:AreaShare | ed:LengthShare | ed:NrOfPoints");

                if (shareEntries == 0) {
                    isValid = false;
                    ValidatorMessage msg = ValidatorMessage.error("Missing entries", "There are no entries for <AreaShare>, <LengthShare>, or <NrOfPoints> at " + xpath.getPath(restriction));
                    result.addMessage("Restriction Validation", msg);
                }
            }

            return isValid;
        } catch (Exception e) {
            logger.error("Error checking restriction on landownership: {}", e.getMessage());
            return false;
        }

    }

    private boolean checkGlossary(Document doc) {
        try {
            List<Node> glossaryNodes = xpath.getNodesList(doc, "//ed:Glossary");

            if (glossaryNodes.isEmpty()) {
                result.addMessage("Glossary Validation", ValidatorMessage.error("Missing entries", "There are no entries for <Glossary>"));
                return false;
            }

            List<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar> glossars = new ArrayList<>(MetadataManager.getV20Konfiguration().getV20KonfigurationGlossar());

            boolean isValid = true;
            for (Node glossaryNode : glossaryNodes) {
                Element glossary = (Element) glossaryNode;

                Node titleLocalisedTextNode = xpath.getNode(glossary, "ed:Title/ed:LocalisedText");
                if (titleLocalisedTextNode == null) continue;

                String titleLanguage = xpath.getString(titleLocalisedTextNode, "ed:Language");
                String titleText = xpath.getString(titleLocalisedTextNode, "ed:Text");

                V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar existingGlossar = null;
                String expectedTitleForMatch = null;

                // Find matching glossary entry:
                // - exact match => OK
                // - extract contains template => WARNING (addition), but still match
                for (V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar glossar : glossars) {
                    var templateTitles = glossar.getTitel()
                            .getLocalisationCHV1MultilingualText()
                            .getLocalisedText()
                            .getLocalisationCHV1LocalisedText();

                    var match = templateTitles.stream()
                            .filter(x -> x.getLanguage().equals(titleLanguage))
                            .filter(x -> x.getText() != null && titleText != null)
                            .filter(x -> titleText.equals(x.getText()) || titleText.contains(x.getText()))
                            .findFirst();

                    if (match.isPresent()) {
                        existingGlossar = glossar;
                        expectedTitleForMatch = match.get().getText();
                        break;
                    }
                }

                if (existingGlossar == null) {
                    continue;
                } else {
                    // If it matched by "contains" (not equals), this is an addition -> WARNING
                    if (expectedTitleForMatch != null && titleText != null && !titleText.equals(expectedTitleForMatch) && titleText.contains(expectedTitleForMatch)) {
                        String msg = "Glossary title contains template text but has additions (" + titleLanguage + "). " + "Expected contained: '" + expectedTitleForMatch + "', Found: '" + titleText + "'";
                        result.addMessage("Glossary Validation",
                                ValidatorMessage.warning("Template Addition", "Title text extended", msg, null)
                        );
                    }
                    glossars.remove(existingGlossar);
                }

                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                // Check Title Translations
                List<Node> titleLocalisedTextNodes = xpath.getNodesList(glossary, "ed:Title/ed:LocalisedText");
                for (Node localisedText : titleLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    var expectedOpt = existingGlossar.getTitel()
                            .getLocalisationCHV1MultilingualText()
                            .getLocalisedText()
                            .getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (expectedOpt.isPresent()) {
                        String expected = expectedOpt.get().getText();

                        if (expected != null && text != null) {
                            if (text.equals(expected)) {
                                // OK
                            } else if (text.contains(expected)) {
                                // Addition => WARNING
                                String msg = "Title text contains template text but has additions (" + language + "). " + "Expected contained: '" + expected + "', Found: '" + text + "'";
                                result.addMessage("Glossary Validation",
                                        ValidatorMessage.warning("Template Addition", "Title text extended", msg, null)
                                );
                            } else {
                                // Not contained => ERROR
                                isValid = false;
                                result.addMessage("Glossary Validation",
                                        ValidatorMessage.error("Incorrect localized text", "Title (" + language + ") does not contain expected template text. Expected: '" + expected + "', Found: '" + text + "'")
                                );
                            }
                        } else {
                            isValid = false;
                            result.addMessage("Glossary Validation",
                                    ValidatorMessage.error("Incorrect localized text", "Title (" + language + ") has empty text where template expects content.")
                            );
                        }
                    } else {
                        // Extra language not defined by template -> WARNING (still consistent with "additions")
                        result.addMessage("Glossary Validation",
                                ValidatorMessage.warning("Template Addition", "Additional translation", "Additional title translation found for language '" + language + "': '" + text + "'", null)
                        );
                    }
                }

                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                // --- Check Content Translations ---
                List<Node> contentLocalisedTextNodes = xpath.getNodesList(glossary, "ed:Content/ed:LocalisedText");
                for (Node localisedText : contentLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    var expectedOpt = existingGlossar.getInhalt()
                            .getLocalisationCHV1MultilingualMText()
                            .getLocalisedText()
                            .getLocalisationCHV1LocalisedMText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (expectedOpt.isPresent()) {
                        String expected = expectedOpt.get().getText();

                        if (expected != null && text != null) {
                            if (text.equals(expected)) {
                                // OK
                            } else if (text.contains(expected)) {
                                // Addition => WARNING
                                String msg = "Content text contains template text but has additions (" + language + "). " + "Expected contained: '" + expected + "', Found: '" + text + "'";
                                result.addMessage("Glossary Validation",
                                        ValidatorMessage.warning("Template Addition", "Content text extended", msg, null)
                                );
                            } else {
                                // Not contained => ERROR
                                isValid = false;
                                result.addMessage("Glossary Validation",
                                        ValidatorMessage.error("Incorrect localized text", "Content (" + language + ") does not contain expected template text. Expected: '" + expected + "', Found: '" + text + "'")
                                );
                            }
                        } else {
                            isValid = false;
                            result.addMessage("Glossary Validation",
                                    ValidatorMessage.error("Incorrect localized text", "Content (" + language + ") has empty text where template expects content.")
                            );
                        }
                    } else {
                        // Extra language not defined by template -> WARNING
                        result.addMessage("Glossary Validation",
                                ValidatorMessage.warning("Template Addition", "Additional translation", "Additional content translation found for language '" + language + "': '" + text + "'", null)
                        );
                    }
                }
            }

            // Missing template entries in extract -> still ERROR (unchanged)
            if (!glossars.isEmpty()) {
                for (V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar glossar : glossars) {
                    String titleLanguage = glossar.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText().getFirst().getLanguage();
                    String title = glossar.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText().getFirst().getText();

                    String msg = "The federal glossary entry with title (" + titleLanguage + ") '" + title + "' is missing in the extract.";
                    result.addMessage("Glossary Validation",
                            ValidatorMessage.error("Metadata Mismatch", "Federal glossary", msg, null)
                    );
                }
                isValid = false;
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean checkDisclaimer(Document doc) {
        try {
            List<Node> disclaimerNodes = xpath.getNodesList(doc, "//ed:Disclaimer");

            if (disclaimerNodes.isEmpty()) {
                ValidatorMessage msg = ValidatorMessage.error("Missing entries", "There are no entries for <Disclaimer>");
                result.addMessage("Disclaimer Validation", msg);
                return false;
            }

            List<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationHaftungshinweis> haftungshinweise = MetadataManager.getV20Konfiguration().getV20KonfigurationHaftungshinweis();

            boolean isValid = true;
            for (Node disclaimerNode : disclaimerNodes) {
                Element disclaimer = (Element) disclaimerNode;

                Node titleLocalisedTextNode = xpath.getNode(disclaimer, "ed:Title/ed:LocalisedText");
                if (titleLocalisedTextNode == null) continue;

                String titleLanguage = xpath.getString(titleLocalisedTextNode, "ed:Language");
                String titleText = xpath.getString(titleLocalisedTextNode, "ed:Text");

                V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationHaftungshinweis existingHaftungshinweis = null;
                for (V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationHaftungshinweis haftungshinweis : haftungshinweise) {
                    if (haftungshinweis.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(titleLanguage) && x.getText().equals(titleText))
                            .count() == 1) {
                        existingHaftungshinweis = haftungshinweis;
                        break;
                    }
                }
                if (existingHaftungshinweis == null) {
                    break;
                } else {
                    haftungshinweise.remove(existingHaftungshinweis);
                }

                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                // Check Title Translations
                List<Node> titleLocalisedTextNodes = xpath.getNodesList(disclaimer, "ed:Title/ed:LocalisedText");
                for (Node localisedText : titleLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    localisationCHV1LocalisedText = existingHaftungshinweis.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (localisationCHV1LocalisedText.isPresent()) {
                        if (!localisationCHV1LocalisedText.get().getText().equals(text)) {
                            isValid = false;
                            result.addMessage("Disclaimer Validation", ValidatorMessage.error("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addMessage("Disclaimer Validation", ValidatorMessage.error("Missing translation", "No localized text found for language '" + language + "'"));
                    }
                }

                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                // Check Content Translations
                List<Node> contentLocalisedTextNodes = xpath.getNodesList(disclaimer, "ed:Content/ed:LocalisedText");
                for (Node localisedText : contentLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    localisationCHV1LocalisedMText = existingHaftungshinweis.getInhalt().getLocalisationCHV1MultilingualMText().getLocalisedText().getLocalisationCHV1LocalisedMText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (localisationCHV1LocalisedMText.isPresent()) {
                        if (!localisationCHV1LocalisedMText.get().getText().equals(text)) {
                            isValid = false;
                            result.addMessage("Disclaimer Validation", ValidatorMessage.error("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedMText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addMessage("Disclaimer Validation", ValidatorMessage.error("Missing translation", "No localized text found for language '" + language + "'"));
                    }
                }
            }
            return isValid;

        } catch (Exception e) {
            logger.error("XPath error in checkDisclaimer: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean checkOffice(Document doc) {
        try {
            List<String> uids = xpath.getNodesList(doc, "//ed:ResponsibleOffice/ed:UID")
                    .stream()
                    .map(org.w3c.dom.Node::getTextContent)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toList();

            if (uids.isEmpty()) {
                logger.debug("No UIDs found in extract to validate.");
                return true;
            }

            boolean allValid = true;
            for (String uid : uids) {
                boolean isValid = ch.swisstopo.oerebchecker.manager.UidManager.validateUID(uid);
                if (!isValid) {
                    allValid = false;
                    String msg = "UID '" + uid + "' is invalid according to BFS Public Services.";
                    result.addMessage("Office Validation", ValidatorMessage.error("Invalid UID", msg));
                    logger.debug(msg);
                } else {
                    logger.trace("UID validated successfully: {}", uid);
                }
            }
            return allValid;
        } catch (Exception e) {
            logger.error("XPath error in checkOffice: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected void postProcess() {
        if (result.StatusCode == ResponseStatusCode.OK && responseFormat == ResponseFormat.xml && result.XmlIsValid != null && result.XmlIsValid) {
            logger.trace("Entering postProcess for XML response.");

            Document doc = getResponseXmlDocument();

            result.RealEstate_DPRIsValid = checkRealEstate_DPR(doc);

            result.GeometryIsValid = checkParamGeometry(doc);
            result.WithImagesIsValid = checkParamWithImages(doc);
            result.LangIsValid = checkParamLang(doc);

            result.RefsAreValid = checkRefs(doc);

            result.TopicsIsValid = checkTopics(doc);
            result.RestrictionOnLandownershipIsValid = checkRestrictionOnLandownership(doc);

            result.ImageAspectRatioIsValid = checkImageAspectRatios(doc);

            result.GlossaryIsValid = checkGlossary(doc);
            result.DisclaimerIsValid = checkDisclaimer(doc);

            result.OfficeIsValid = checkOffice(doc);
        }
    }
}