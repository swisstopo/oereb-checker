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
            uri = RequestHelper.buildUri(urlTemplate, this.basicUri, responseFormat, requestParams);
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

    private void runGetCapabilities() {
        if (capabilitiesSyncComplete) {
            logger.trace("Capabilities already synced, skipping.");
            return;
        }

        capabilitiesLock.lock();

        try {
            if (!capabilitiesSyncComplete) {
                GetCapabilitiesConfig config = new GetCapabilitiesConfig();
                config.FORMAT = ResponseFormat.xml.name();
                config.ExpectedStatusCode = 200;

                new GetCapabilities(basicUri, config).run();
                capabilitiesSyncComplete = true;
            }
        } catch (Exception e) {
            logger.error("Failed to load capabilities: {}", e.getMessage(), e);
        } finally {
            capabilitiesLock.unlock();
        }
    }

    private boolean checkRealEstate_DPR(Document doc) {
        try {
            String egrid = xpath.getString(doc, "//ed:RealEstate/ed:EGRID");
            String identDN = xpath.getString(doc, "//ed:RealEstate/ed:IdentDN");
            String number = xpath.getString(doc, "//ed:RealEstate/ed:Number");

            boolean egridMatch = egrid.equals(requestParams.get("EGRID"));
            boolean identMatch = !identDN.isBlank() && !number.isBlank();

            return egridMatch || identMatch;
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
            request = HttpRequest.newBuilder().uri(new URI(uri)).build();
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
                logger.debug("Failed to load reference '{}' (HTTP {}). Location: {}", uri, response.statusCode(), xpath.getPath(node));
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
                return geometryCount == restrictionCount;
            } else {
                return geometryCount == 0;
            }
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

            result.addImageValidationError(new ValidatorMessage(title, message));
            logger.debug("{}", message);
        }

        BufferedImage image = ImageIO.read(inputStream);
        float existingAspectRatio = (float) image.getHeight() / (float) image.getWidth();

        logger.trace("Image dimensions: {}x{}. Calculated aspect ratio: {}", image.getWidth(), image.getHeight(), existingAspectRatio);

        boolean isAspectRatioValid = expectedAspectRatio == existingAspectRatio;
        if (!isAspectRatioValid) {
            String title = "Invalid image aspect ratio";
            String message = "The image aspect ratio is invalid. Expected: " + expectedAspectRatio + ", Found: " + existingAspectRatio + " at node: " + xpath.getPath(imageNode);

            result.addImageValidationError(new ValidatorMessage(title, message));
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
                            result.addImageValidationError(new ValidatorMessage("Invalid image", "The image aspect ratio is invalid or it is not a PNG at node: " + xpath.getPath(parentNode)));
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
                        boolean nodeIsValid = validateAndCacheRemoteImage(uri, localisedUri, 99, 174, checkedMap);
                        if (!nodeIsValid) {
                            result.addImageValidationError(new ValidatorMessage("Invalid image", "The image aspect ratio is invalid or it is not a PNG at node: " + xpath.getPath(localisedUri.getParentNode())));
                        }
                        isValid = nodeIsValid && isValid;
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
                        boolean nodeIsValid = validateAndCacheRemoteImage(uri, symbolRef, 3, 6, checkedMap);
                        if (!nodeIsValid) {
                            result.addImageValidationError(new ValidatorMessage("Invalid image", "The image aspect ratio is invalid or it is not a PNG at node: " + xpath.getPath(symbolRef.getParentNode())));
                        }
                        isValid = nodeIsValid && isValid;
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
                logger.warn("Failed to load image for aspect ratio check: {} (Status {})", uri, response.statusCode());
            }
        }
        return cache.get(uri);
    }

    private boolean checkParamLang(Document doc) {

        if (getCapabilitiesLanguages.isEmpty()) {
            if (capabilitiesSyncComplete) {
                try {
                    logger.trace("Waiting for capabilities synchronization...");
                    capabilitiesLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while waiting for capabilities: {}", e.getMessage());
                }
            } else {
                runGetCapabilities();
            }
        }

        if (getCapabilitiesLanguages.isEmpty()) {
            logger.error("Language check failed: No languages found in GetCapabilities response");
            return false;
        }

        try {
            List<String> foundLanguages = xpath.getNodesList(doc, "//*[local-name()='Language']")
                    .stream()
                    .map(Node::getTextContent)
                    .distinct()
                    .toList();

            if (StringUtils.isNotBlank(config.LANG)) {
                return foundLanguages.contains(config.LANG) && foundLanguages.size() == 1;
            } else {
                List<String> missingLanguages = new ArrayList<>(getCapabilitiesLanguages);
                missingLanguages.removeAll(foundLanguages);

                return missingLanguages.isEmpty();
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
                result.addTopicsValidationError(new ValidatorMessage("No topics available", "There are no entries for <ConcernedTheme>, <NotConcernedTheme>, or <ThemeWithoutData>."));
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

                    if (federalLocalisedText != null && !federalLocalisedText.getText().equals(text)) {
                        titleIsValid = false;
                        String msg = "Theme '" + code + "': Title mismatch for language '" + lang + "'. Expected: '" + federalLocalisedText.getText() + "', Found: '" + text + "'";
                        result.addTopicsValidationError(new ValidatorMessage("Incorrect theme title", msg));
                        logger.debug(msg);
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

    private boolean checkTopicCodesAgainstCapabilities(List<String> extractThemeCodes) {
        try {
            logger.trace("Comparing extract theme codes ({}) against capabilities topic codes.", extractThemeCodes);

            if (getCapabilitiesTopicCodes.isEmpty()) {
                if (capabilitiesSyncComplete) {
                    try {
                        logger.trace("Waiting for capabilities synchronization...");
                        capabilitiesLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while waiting for capabilities: {}", e.getMessage());
                    }
                } else {
                    runGetCapabilities();
                }
            }
            if (getCapabilitiesTopicCodes.isEmpty()) {
                logger.error("Theme check failed: No topics found in GetCapabilities response");
                return false;
            }

            List<String> themeNotInGetCapabilities = new ArrayList<>();

            for (String extractThemeCode : extractThemeCodes) {
                if (!getCapabilitiesTopicCodes.contains(extractThemeCode)) {
                    themeNotInGetCapabilities.add(extractThemeCode);
                    result.addTopicsValidationError(new ValidatorMessage("Theme missing from capabilities", "Extract theme '" + extractThemeCode + "' is not present in GetCapabilities response"));
                }
            }

            if (StringUtils.isBlank(config.TOPICS) || config.TOPICS.equalsIgnoreCase("ALL")) {

                List<String> themeNotInExtract = new ArrayList<>();

                for (String themeCode : getCapabilitiesTopicCodes) {
                    if (!extractThemeCodes.contains(themeCode)) {
                        themeNotInExtract.add(themeCode);
                        result.addTopicsValidationError(new ValidatorMessage("Capabilities theme missing in extract", "Capabilities theme '" + themeCode + "' is not present in extract"));
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
                        result.addTopicsValidationError(new ValidatorMessage("Theme missing from federal topics", "Extract theme '" + extractThemeCode + "' is not present in federal topics"));
                    }
                }
                for (String themeCode : federalTopicCodes) {
                    if (!extractThemeCodes.contains(themeCode)) {
                        federalThemeNotInExtract.add(themeCode);
                        result.addTopicsValidationError(new ValidatorMessage("Federal theme missing in extract", "Federal theme '" + themeCode + "' is not present in extract"));
                    }
                }

                return themeNotInFederalTopics.isEmpty() && federalThemeNotInExtract.isEmpty();

            } else {

                List<String> requestedThemeCodes = Arrays.asList(config.TOPICS.split(","));

                List<String> missingRequestedThemeCodes = new ArrayList<>(requestedThemeCodes);
                missingRequestedThemeCodes.removeAll(extractThemeCodes);
                for (String themeCode : missingRequestedThemeCodes) {
                    result.addTopicsValidationError(new ValidatorMessage("Requested theme missing in extract", "Requested theme '" + themeCode + "' is not present in extract"));
                }

                List<String> additionalExtractThemeCodes = new ArrayList<>(extractThemeCodes);
                additionalExtractThemeCodes.removeAll(requestedThemeCodes);
                for (String themeCode : additionalExtractThemeCodes) {
                    result.addTopicsValidationError(new ValidatorMessage("Unrequested theme in extract", "Theme '" + themeCode + "' is not requested but present in extract"));
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
                return false;
            }

            String restrictionQuery = String.format("//ed:RestrictionOnLandownership[ed:Theme/ed:Code='%s']", topicCode);
            Node restriction = xpath.getNode(doc, restrictionQuery);

            List<ValidatorMessage> errors = FederalTopicManager.checkFederalTopicInformation(
                    MetadataManager.getFederalTopicInformation().get(topicCode).getDokumente().stream().toList(),
                    legalProvisionsNodes,
                    xpath.getPath(restriction)
            );
            errors.forEach(result::addFederalTopicLawDocumentsError);
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
                    ValidatorMessage msg = new ValidatorMessage("Missing entries", "There are no entries for <AreaShare>, <LengthShare>, or <NrOfPoints> at " + xpath.getPath(restriction));
                    result.addRestrictionOnLandownershipValidationError(msg);
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
                ValidatorMessage msg = new ValidatorMessage("Missing entries", "There are no entries for <Glossary>");
                result.addGlossaryValidationError(msg);
                return false;
            }

            List<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar> glossars = MetadataManager.getV20Konfiguration().getV20KonfigurationGlossar();

            boolean isValid = true;
            for (Node glossaryNode : glossaryNodes) {
                Element glossary = (Element) glossaryNode;

                Node titleLocalisedTextNode = xpath.getNode(glossary, "ed:Title/ed:LocalisedText");
                if (titleLocalisedTextNode == null) continue;

                String titleLanguage = xpath.getString(titleLocalisedTextNode, "ed:Language");
                String titleText = xpath.getString(titleLocalisedTextNode, "ed:Text");

                V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar existingGlossar = null;

                for (V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar glossar : glossars) {
                    if (glossar.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(titleLanguage) && x.getText().equals(titleText))
                            .count() == 1) {
                        existingGlossar = glossar;
                        break;
                    }
                }
                if (existingGlossar == null) {
                    break;
                } else {
                    glossars.remove(existingGlossar);
                }

                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                // Check Title Translations
                List<Node> titleLocalisedTextNodes = xpath.getNodesList(glossary, "ed:Title/ed:LocalisedText");
                for (Node localisedText : titleLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    localisationCHV1LocalisedText = existingGlossar.getTitel().getLocalisationCHV1MultilingualText().getLocalisedText().getLocalisationCHV1LocalisedText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (localisationCHV1LocalisedText.isPresent()) {
                        if (!localisationCHV1LocalisedText.get().getText().equals(text)) {
                            isValid = false;
                            result.addGlossaryValidationError(new ValidatorMessage("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addGlossaryValidationError(new ValidatorMessage("Missing translation", "No localized text found for language '" + language + "'"));
                    }
                }


                Optional<V20Texte.DATASECTION.V20Konfiguration.V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                // Check Content Translations
                List<Node> contentLocalisedTextNodes = xpath.getNodesList(glossary, "ed:Content/ed:LocalisedText");
                for (Node localisedText : contentLocalisedTextNodes) {
                    String language = xpath.getString(localisedText, "ed:Language");
                    String text = xpath.getString(localisedText, "ed:Text");

                    localisationCHV1LocalisedMText = existingGlossar.getInhalt().getLocalisationCHV1MultilingualMText().getLocalisedText().getLocalisationCHV1LocalisedMText()
                            .stream()
                            .filter(x -> x.getLanguage().equals(language))
                            .findFirst();

                    if (localisationCHV1LocalisedMText.isPresent()) {
                        if (!localisationCHV1LocalisedMText.get().getText().equals(text)) {
                            isValid = false;
                            result.addGlossaryValidationError(new ValidatorMessage("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedMText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addGlossaryValidationError(new ValidatorMessage("Missing translation", "No localized text found for language '" + language + "'"));
                    }
                }
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
                ValidatorMessage msg = new ValidatorMessage("Missing entries", "There are no entries for <Disclaimer>");
                result.addDisclaimerValidationError(msg);
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
                            result.addGlossaryValidationError(new ValidatorMessage("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addGlossaryValidationError(new ValidatorMessage("Missing translation", "No localized text found for language '" + language + "'"));
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
                            result.addGlossaryValidationError(new ValidatorMessage("Incorrect localized text", "The localized text '" + text + "' should be '" + localisationCHV1LocalisedMText.get().getText() + "'"));
                        }
                    } else {
                        isValid = false;
                        result.addGlossaryValidationError(new ValidatorMessage("Missing translation", "No localized text found for language '" + language + "'"));
                    }
                }
            }
            return isValid;

        } catch (Exception e) {
            logger.error("XPath error in checkDisclaimer: {}", e.getMessage(), e);
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
            /*
            Aktuell noch nicht benötigt, da wir die UID nicht mit einer API abgleichen.
            result.OfficeIsValid = checkOffice(doc); // 5.10
            */
        }
    }
}