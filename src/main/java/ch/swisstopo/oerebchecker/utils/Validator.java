package ch.swisstopo.oerebchecker.utils;

import ch.swisstopo.oerebchecker.checks.GetVersions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.validators.ValidatorConfig;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    private static boolean pdfIsInitialised = false;

    private static List<StreamSource> getAvailableXsdStreamSources() {
        List<StreamSource> streamSources = new LinkedList<>();

        streamSources.add(new StreamSource(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("ch/swisstopo/oereb/Versioning.xsd"),
                "http://schemas.geo.admin.ch/V_D/OeREB/1.0/Versioning")
        );
        streamSources.add(new StreamSource(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("ch/swisstopo/oereb/Extract.xsd"),
                "http://schemas.geo.admin.ch/V_D/OeREB/2.0/Extract")
        );
        streamSources.add(new StreamSource(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("ch/swisstopo/oereb/ExtractData.xsd"),
                "http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData")
        );
        streamSources.add(new StreamSource(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("ch/swisstopo/oereb/geometry.xsd"),
                "http://www.interlis.ch/geometry/1.0")
        );
        streamSources.add(new StreamSource(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("ch/swisstopo/oereb/xmldsig-core-schema.xsd"),
                "http://www.w3.org/2000/09/xmldsig#")
        );

        return streamSources;
    }

    private static void initialisePdf() {
        if (!pdfIsInitialised) {
            pdfIsInitialised = true;
            VeraGreenfieldFoundryProvider.initialise();
        }
    }

    private static ValidatorConfig getPdfValidatorConfig() {
        return ValidatorFactory.createConfig(
                PDFAFlavour.NO_FLAVOUR,
                PDFAFlavour.PDFA_1_A,
                false,
                -1,
                false,
                false,
                Level.OFF,
                100,
                false,
                "",
                false,
                false);
    }

    public static ValidatorResult checkXml(InputStream inputStream) {

        ValidatorResult result = new ValidatorResult();
        XmlErrorHandler xsdErrorHandler = new XmlErrorHandler();

        try {
            Schema xmlSchema;
            List<StreamSource> xsdStreamSources = getAvailableXsdStreamSources();
            if (xsdStreamSources.size() > 1) {
                xmlSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdStreamSources.toArray(new Source[0]));
            } else {
                xmlSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdStreamSources.getFirst());
            }
            javax.xml.validation.Validator xmlValidator = xmlSchema.newValidator();
            xmlValidator.setErrorHandler(xsdErrorHandler);
            xmlValidator.validate(new StreamSource(inputStream));
            result.IsValid = xsdErrorHandler.getExceptions().isEmpty();
        } catch (SAXException | IOException e) {
            result.IsValid = false;
            logger.error("Error while validating the XSD file.", e);
        }

        xsdErrorHandler.getExceptions().forEach(e -> {
            String message = String.format("Line number: %s, Column number: %s. %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            result.addMessage(message);
        });

        return result;
    }

    public static ValidatorResult checkPdf(InputStream inputStream) {

        initialisePdf();

        ValidatorResult result = new ValidatorResult();

        // https://docs.verapdf.org/develop/
        List<PDFAFlavour> flavours = new LinkedList<>();
        flavours.add(PDFAFlavour.PDFA_1_A);
        flavours.add(PDFAFlavour.PDFA_1_B);
        flavours.add(PDFAFlavour.PDFA_2_A);

        try (VeraPDFFoundry foundry = Foundries.defaultInstance();
             PDFAParser parser = foundry.createParser(inputStream);
             PDFAValidator validator = foundry.createValidator(getPdfValidatorConfig(), flavours)) {

            List<ValidationResult> validationResults = validator.validateAll(parser);
            for (ValidationResult validationResult : validationResults) {
                if (validationResult.isCompliant()) { // File is a valid
                    result.IsValid = true;
                } else {
                    List<TestAssertion> testAssertions = validationResult.getTestAssertions();
                    for (TestAssertion testAssertion : testAssertions) {
                        result.addMessage(validationResult.getPDFAFlavour().toString(), testAssertion.getRuleId().toString(), testAssertion.getMessage(), testAssertion.getLocation().toString());
                    }
                    result.IsValid = result.IsValid != null && result.IsValid;
                }
            }
        } catch (Exception ex) {
            result.IsValid = false;
            logger.error("Error: {}", ex.getMessage(), ex);
        }

        return result;
    }
}
