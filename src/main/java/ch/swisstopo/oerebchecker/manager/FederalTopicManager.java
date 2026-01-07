package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oereb.V20Gesetze;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.utils.XPathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class FederalTopicManager {
    protected static final Logger logger = LoggerFactory.getLogger(FederalTopicManager.class);

    private static final XPathHelper xpath = new XPathHelper();

    public static List<ValidatorMessage> checkFederalTopicInformation(
            List<V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument> federalDocuments,
            NodeList legalProvisionsNodes,
            String contextXPath) {

        logger.trace("Starting federal topic check for {} provisions against {} federal documents.", legalProvisionsNodes.getLength(), federalDocuments.size());
        List<ValidatorMessage> errors = new ArrayList<>();

        for (int i = 0; i < legalProvisionsNodes.getLength(); i++) {
            Element legalProvision = (Element) legalProvisionsNodes.item(i);
            String officialNumber = getOfficialNumberFromElement(legalProvision);
            logger.trace("Searching for federal document match with OfficialNumber: '{}'", officialNumber);

            Optional<V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument> match = federalDocuments.stream()
                    .filter(fed -> fed.getOffizielleNr().getLocalisationCHV1MultilingualText().getLocalisedText()
                            .stream().anyMatch(t -> t.getLocalisationCHV1LocalisedText().getText().equals(officialNumber))
                    )
                    .findFirst();

            if (match.isPresent()) {
                logger.trace("Match found for OfficialNumber '{}'. Proceeding with multilingual comparison.", officialNumber);
                errors.addAll(compareLegalProvisionToFederalTopicDocument(legalProvision, match.get(), contextXPath));
            } else {
                logger.trace("No federal document match found for OfficialNumber '{}'.", officialNumber);
            }
        }

        return errors;
    }

    private static String getOfficialNumberFromElement(Element el) {
        try {
            return xpath.getString(el, "ed:OfficialNumber/ed:Text");
        } catch (Exception e) {
            logger.error("XPath error in getOfficialNumberFromElement: {}", e.getMessage());
            return "";
        }
    }

    private static List<ValidatorMessage> compareLegalProvisionToFederalTopicDocument(
            Element legalProvision,
            V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument fedDoc,
            String contextXPath) {

        List<ValidatorMessage> errors = new ArrayList<>();

        errors.addAll(compareMultilingual(legalProvision, "Title", fedDoc.getTitel().getLocalisationCHV1MultilingualText(), contextXPath));
        errors.addAll(compareMultilingual(legalProvision, "Abbreviation", fedDoc.getAbkuerzung().getLocalisationCHV1MultilingualText(), contextXPath) );
        errors.addAll(compareMultilingual(legalProvision, "TextAtWeb", fedDoc.getTextImWeb().getLocalisationCHV1MultilingualUri(), contextXPath) );

        return errors;
    }

    private static List<ValidatorMessage> compareMultilingual(Element parent, String tagName, V20Gesetze.LocalisationTextContainer multilingualText, String contextXPath) {

        List<ValidatorMessage> errors = new ArrayList<>();

        if (multilingualText == null) {
            return errors;
        }

        try {
            List<Node> localisedTextNodes = xpath.getNodesList(parent, "ed:" + tagName + "/ed:LocalisedText");
            if (localisedTextNodes.isEmpty()) {
                return errors;
            }

            for (Node node : localisedTextNodes) {
                String language = xpath.getString(node, "ed:Language");
                String text = xpath.getString(node, "ed:Text");

                var match = multilingualText.getLocalisedText()
                        .stream()
                        .filter(x -> x.getLocalisationCHV1LocalisedText().getLanguage().equals(language))
                        .findFirst();
                if (match.isPresent()) {
                    String expectedText = match.get().getLocalisationCHV1LocalisedText().getText();
                    if (!expectedText.equals(text)) {
                        String message = String.format("Content mismatch for tag '%s' in language '%s'. Expected: '%s', Actual: '%s' at %s", tagName, language, expectedText, text, contextXPath);
                        errors.add(new ValidatorMessage("Federal document content mismatch", message));
                        logger.error(message);
                    }
                }
            }
            return errors;

        } catch (Exception e) {
            logger.error("XPath error in compareMultilingual: {}", e.getMessage());
            return errors;
        }
    }

    private static List<ValidatorMessage> compareMultilingual(Element parent, String tagName, V20Gesetze.LocalisedUriContainer multilingualUri, String contextXPath) {

        List<ValidatorMessage> errors = new ArrayList<>();

        if (multilingualUri == null) {
            return errors;
        }

        try {
            List<Node> localisedTextNodes = xpath.getNodesList(parent, "ed:" + tagName + "/ed:LocalisedText");
            if (localisedTextNodes.isEmpty()) {
                return errors;
            }

            for (Node node : localisedTextNodes) {
                String language = xpath.getString(node, "ed:Language");
                String text = xpath.getString(node, "ed:Text");

                var match = multilingualUri.getLocalisedUri()
                        .stream()
                        .filter(x -> x.getOeREBKRMV20LocalisedUri().getLanguage().equals(language))
                        .findFirst();
                if (match.isPresent()) {
                    String expectedUri = match.get().getOeREBKRMV20LocalisedUri().getText();
                    if (!expectedUri.equals(text)) {
                        String message = String.format("URI mismatch for tag '%s' in language '%s'. Expected: '%s', Actual: '%s' at %s", tagName, language, expectedUri, text, contextXPath);
                        errors.add(new ValidatorMessage("Federal document URI mismatch", message));
                        logger.error(message);
                    }
                }
            }
            return errors;

        }catch (Exception ex){
            logger.error("XPath error in compareMultilingual: {}", ex.getMessage());
            return errors;
        }
    }
}

