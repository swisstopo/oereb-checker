package ch.swisstopo.oerebchecker.checks;

import ch.swisstopo.oerebchecker.configs.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.configs.GetExtractByIdConfig;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.utils.ResponseFormat;
import ch.swisstopo.oerebchecker.utils.ResponseStatusCode;
import ch.swisstopo.oerebchecker.utils.ValidatorMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.utils.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class GetExtractById extends Check {

    private final URI basicUri;
    private final GetExtractByIdConfig config;

    private URL url;
    private boolean canRun = false;


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
            url = getUrl(this.basicUri);
            if (url != null) {
                canRun = true;
                result = new CheckResult(url);
            }
        }
    }

    protected boolean validateConfig(GetExtractByIdConfig config) {

        if (!super.validateConfig(config)) {
            return false;
        }

        if (config.GEOMETRY) {
            requestParams.put("GEOMETRY", true);
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
        if (config.WITHIMAGES) {
            requestParams.put("WITHIMAGES", true);
        }

        int variants = 0;
        // request variant B
        if (StringUtils.isNotBlank(config.IDENTDN) && config.NUMBER != null) {
            requestParams.put("IDENTDN", config.IDENTDN);
            requestParams.put("NUMBER", config.NUMBER);
            variants++;
        }
        // request variant A
        if (StringUtils.isNotBlank(config.EGRID)) {
            requestParams.put("EGRID", config.EGRID);
            variants++;
        } else if (!config.EGRIDS.isEmpty()) {
            Random random = new Random();
            requestParams.put("EGRID", config.EGRIDS.get(random.nextInt(config.EGRIDS.size() - 1)));
            variants++;
        }

        if (variants != 1) {
            logger.error("[{}] - GetExtractById error: invalid request variants - count: '{}'", uUId, variants);
        }
        return variants == 1;
    }

    private void runGetCapabilities() {

        GetCapabilitiesConfig config = new GetCapabilitiesConfig();
        config.FORMAT = ResponseFormat.xml.name();
        config.ExpectedStatusCode = 200;

        new GetCapabilities(basicUri, config).run();
    }

    protected boolean checkEGRID(Document doc) {

        String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");

        String prefix = eDPrefix != null ? eDPrefix + ":" : "";
        String egridTag = prefix + "EGRID";

        NodeList egridNodes = doc.getElementsByTagName(egridTag);

        if (egridNodes.getLength() > 0) {
            Element egrid = (Element) egridNodes.item(0);
            return egrid.getTextContent().equals(requestParams.get("EGRID"));
        }
        return false;
    }

    protected boolean checkGeometry(Document doc) {

        String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");
        eDPrefix = eDPrefix != null ? eDPrefix + ":" : "";

        int geometryCount = 0;
        geometryCount += doc.getElementsByTagName(eDPrefix + "Point").getLength();
        geometryCount += doc.getElementsByTagName(eDPrefix + "Line").getLength();
        geometryCount += doc.getElementsByTagName(eDPrefix + "Surface").getLength();
        if (config.GEOMETRY) {
            NodeList restrictionOnLandownershipNodes = doc.getElementsByTagName(eDPrefix + "RestrictionOnLandownership");
            return geometryCount == restrictionOnLandownershipNodes.getLength();
        } else {
            return geometryCount == 0;
        }
    }

    protected boolean checkLang(Document doc) {

        if (getCapabilitiesLanguages.isEmpty()) {
            runGetCapabilities();
        }
        if (getCapabilitiesLanguages.isEmpty()) {
            logger.error("[{}] - GetExtractById checkLang error: {}", uUId, "no languages from getCapabilities");
            return false;
        }

        String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");
        eDPrefix = eDPrefix != null ? eDPrefix + ":" : "";

        String language;
        Map<String, Integer> languagesCount = new HashMap<>();
        NodeList languageNodes = doc.getElementsByTagName(eDPrefix + "Language");
        for (int i = 0; i < languageNodes.getLength(); i++) {
            language = languageNodes.item(i).getTextContent();
            if (languagesCount.containsKey(language)) {
                languagesCount.replace(language, languagesCount.get(language) + 1);
            } else {
                languagesCount.put(language, 1);
            }
        }

        if (StringUtils.isNotBlank(config.LANG)) {
            return languagesCount.containsKey(config.LANG) && languagesCount.size() == 1;
        } else {
            List<String> missingLanguages = new ArrayList<>(getCapabilitiesLanguages);
            missingLanguages.removeAll(languagesCount.keySet());

            return missingLanguages.isEmpty();
        }
    }

    protected boolean checkTopics(Document doc) {

        try {
            if (getCapabilitiesTopicCodes.isEmpty()) {
                runGetCapabilities();
            }
            if (getCapabilitiesTopicCodes.isEmpty()) {
                logger.error("[{}] - GetExtractById checkThemes error: {}", uUId, "no topics from getCapabilities");
                return false;
            }

            String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");

            String prefix = eDPrefix != null ? eDPrefix + ":" : "";
            String concernedThemeTag = prefix + "ConcernedTheme";
            String notConcernedThemeTag = prefix + "NotConcernedTheme";
            String themeWithoutDataTag = prefix + "ThemeWithoutData";

            String themeCodeTag = prefix + "Code";

            NodeList concernedThemeNodes = doc.getElementsByTagName(concernedThemeTag);
            NodeList notConcernedThemeNodes = doc.getElementsByTagName(notConcernedThemeTag);
            NodeList themeWithoutDataNodes = doc.getElementsByTagName(themeWithoutDataTag);

            List<String> extractThemeCodes = new ArrayList<>();

            for (int i = 0; i < concernedThemeNodes.getLength(); i++) {
                Element theme = (Element) concernedThemeNodes.item(i);
                String code = theme.getElementsByTagName(themeCodeTag).item(0).getTextContent();
                extractThemeCodes.add(code);
            }
            for (int i = 0; i < notConcernedThemeNodes.getLength(); i++) {
                Element theme = (Element) notConcernedThemeNodes.item(i);
                String code = theme.getElementsByTagName(themeCodeTag).item(0).getTextContent();
                extractThemeCodes.add(code);
            }
            for (int i = 0; i < themeWithoutDataNodes.getLength(); i++) {
                Element theme = (Element) themeWithoutDataNodes.item(i);
                String code = theme.getElementsByTagName(themeCodeTag).item(0).getTextContent();
                extractThemeCodes.add(code);
            }

            List<String> themeNotInGetCapabilities = new ArrayList<>();
            List<String> themeNotInExtract = new ArrayList<>();
            for (String extractThemeCode : extractThemeCodes) {
                if (!getCapabilitiesTopicCodes.contains(extractThemeCode)) {
                    themeNotInGetCapabilities.add(extractThemeCode);
                    result.addTopicsValidationError(new ValidatorMessage("extract theme not in capabilities", "extract theme '" + extractThemeCode + "' not in capabilities"));
                }
            }
            for (String themeCode : getCapabilitiesTopicCodes) {
                if (!extractThemeCodes.contains(themeCode)) {
                    themeNotInExtract.add(themeCode);
                    result.addTopicsValidationError(new ValidatorMessage("capabilities theme not in extract", "capabilities theme '" + themeCode + "' not in extract"));
                }
            }

            if (StringUtils.isBlank(config.TOPICS) || config.TOPICS.equalsIgnoreCase("ALL")) {
                return themeNotInGetCapabilities.isEmpty() && themeNotInExtract.isEmpty();

            } else if (config.TOPICS.equalsIgnoreCase("ALL_FEDERAL")) {
                // TODO: define federal services
                return false;

            } else {
                List<String> requestedThemeCodes = Arrays.asList(config.TOPICS.split(","));

                List<String> missingRequestedThemeCodes = new ArrayList<>(requestedThemeCodes);
                missingRequestedThemeCodes.removeAll(extractThemeCodes);
                List<String> additionalExtractThemeCodes = new ArrayList<>(extractThemeCodes);
                missingRequestedThemeCodes.removeAll(requestedThemeCodes);

                return missingRequestedThemeCodes.isEmpty() && additionalExtractThemeCodes.isEmpty();
            }

        } catch (Exception e) {
            logger.error("[{}] - GetExtractById error: {}", uUId, e.getMessage(), e);
        }

        return false;
    }

    protected Boolean checkWithImages(Document doc) {

        String eDPrefix = doc.lookupPrefix("http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData");

        String prefix = eDPrefix != null ? eDPrefix + ":" : "";
        String imageTag = prefix + "Image";

        String symbolTag = prefix + "Symbol";
        String symbolRefTag = symbolTag + "Ref";
        String logoPLRCadastreTag = prefix + "LogoPLRCadastre";
        String logoPLRCadastreRefTag = logoPLRCadastreTag + "Ref";
        String federalLogoTag = prefix + "FederalLogo";
        String federalLogoRefTag = federalLogoTag + "Ref";
        String cantonalLogoTag = prefix + "CantonalLogo";
        String cantonalLogoRefTag = cantonalLogoTag + "Ref";
        String municipalityLogoTag = prefix + "MunicipalityLogo";
        String municipalityLogoRefTag = municipalityLogoTag + "Ref";
        String qrCodeTag = prefix + "QRCode";
        String qrCodeRefTag = qrCodeTag + "Ref";

        List<NodeList> nodes = new ArrayList<>();
        nodes.add(doc.getElementsByTagName(imageTag));
        nodes.add(doc.getElementsByTagName(symbolTag));
        nodes.add(doc.getElementsByTagName(logoPLRCadastreTag));
        nodes.add(doc.getElementsByTagName(federalLogoTag));
        nodes.add(doc.getElementsByTagName(cantonalLogoTag));
        nodes.add(doc.getElementsByTagName(municipalityLogoTag));
        nodes.add(doc.getElementsByTagName(qrCodeTag));
        int imageCount = 0;
        for (NodeList nodeList : nodes) {
            imageCount += nodeList.getLength();
        }

        nodes.clear();
        nodes.add(doc.getElementsByTagName(symbolRefTag));
        nodes.add(doc.getElementsByTagName(logoPLRCadastreRefTag));
        nodes.add(doc.getElementsByTagName(federalLogoRefTag));
        nodes.add(doc.getElementsByTagName(cantonalLogoRefTag));
        nodes.add(doc.getElementsByTagName(municipalityLogoRefTag));
        nodes.add(doc.getElementsByTagName(qrCodeRefTag));
        int refCount = 0;
        for (NodeList nodeList : nodes) {
            refCount += nodeList.getLength();
        }

        if (config.WITHIMAGES) {
            return refCount == 0 && imageCount > 0;
        } else {
            return imageCount == 0 && refCount > 0;
        }
    }

    public CheckResult run() {
        if (canRun) {
            logger.info("[{}] - GetExtractById start: {}", uUId, url);

            HttpClient client = null;
            try {
                client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                checkResponse(response);

                if (responseFormat == ResponseFormat.xml) {

                    Document doc = getResponseXmlDocument();
                    result.EGRIDIsValid = checkEGRID(doc);
                    result.GeometryIsValid = checkGeometry(doc);
                    result.LangIsValid = checkLang(doc);
                    result.TopicsIsValid = checkTopics(doc);
                    result.WithImagesIsValid = checkWithImages(doc);
                }

            } catch (Exception e) {
                if (e instanceof IOException ioe && ioe.getMessage().equals("unexpected content length header with 204 response")) {
                    result.StatusCode = ResponseStatusCode.NO_CONTENT;
                    result.StatusCodeCorrect = responseStatusCode == ResponseStatusCode.NO_CONTENT;
                    result.ContentTypeCorrect = null; // because no content-type needed
                } else {
                    result.HasError = true;
                    result.ErrorMessage = e.getMessage();
                    logger.error("[{}] - GetExtractById error: {}", uUId, e.getMessage(), e);
                }
            } finally {
                if (client != null) {
                    client.shutdownNow();
                    client.close();
                }
            }

            logger.info("[{}] - GetExtractById done: {}", uUId, url);
            return result;
        }
        return null;
    }
}
