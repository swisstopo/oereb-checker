package ch.swisstopo.oerebchecker.results;

import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CheckResult {

    public String Url;

    public int StatusCode;
    public boolean StatusCodeCorrect = false;

    public String ContentType;
    public Boolean ContentTypeCorrect = false;

    public Boolean XmlIsValid = null;
    public List<ValidatorMessage> XsdValidationErrors = null;

    public Boolean SupportedVersionIsValid = null;

    public Boolean RealEstate_DPRIsValid = null;
    public Boolean RefsAreValid = null;
    public Boolean GeometryIsValid = null;
    public Boolean LangIsValid = null;

    public Boolean TopicsIsValid = null;
    public List<ValidatorMessage> TopicsValidationErrors = null;

    public Boolean FederalTopicLawDocumentsIsValid = null;
    public List<ValidatorMessage> FederalTopicLawDocumentErrors = null;

    public Boolean RestrictionOnLandownershipIsValid = null;
    public List<ValidatorMessage> RestrictionOnLandownershipValidationErrors = null;

    public Boolean ImageAspectRatioIsValid = null;
    public List<ValidatorMessage> ImageSizesValidationErrors = null;

    public Boolean GlossaryIsValid = null;
    public List<ValidatorMessage> GlossaryValidationErrors = null;

    public Boolean DisclaimerIsValid = null;
    public List<ValidatorMessage> DisclaimerValidationErrors = null;

    public Boolean WithImagesIsValid = null;

    public Boolean PdfIsValid = null;
    public List<ValidatorMessage> PdfValidationErrors = null;

    public boolean Successful = false;

    public Boolean HasError = null;
    public String ErrorMessage = null;

    public CheckResult(URI uri) {
        Url = uri.toString();
    }

    public void calculateResult() {
        Successful =
                StatusCodeCorrect &&
                        (ContentTypeCorrect == null || ContentTypeCorrect) &&
                        (XmlIsValid == null || XmlIsValid) &&
                        (PdfIsValid == null || PdfIsValid);
    }

    public void addXsdValidationFailure(ValidatorMessage validatorMessage) {
        if (XsdValidationErrors == null) {
            XsdValidationErrors = new ArrayList<>();
        }
        XsdValidationErrors.add(validatorMessage);
    }

    public void addTopicsValidationError(ValidatorMessage validatorMessage) {
        if (TopicsValidationErrors == null) {
            TopicsValidationErrors = new ArrayList<>();
        }
        TopicsValidationErrors.add(validatorMessage);
    }

    public void addFederalTopicLawDocumentsError(ValidatorMessage validatorMessage) {
        if (FederalTopicLawDocumentErrors == null) {
            FederalTopicLawDocumentErrors = new ArrayList<>();
        }
        FederalTopicLawDocumentErrors.add(validatorMessage);
    }

    public void addRestrictionOnLandownershipValidationError(ValidatorMessage validatorMessage) {
        if (RestrictionOnLandownershipValidationErrors == null) {
            RestrictionOnLandownershipValidationErrors = new ArrayList<>();
        }
        RestrictionOnLandownershipValidationErrors.add(validatorMessage);
    }

    public void addImageValidationError(ValidatorMessage validatorMessage) {
        if (ImageSizesValidationErrors == null) {
            ImageSizesValidationErrors = new ArrayList<>();
        }
        ImageSizesValidationErrors.add(validatorMessage);
    }

    public void addGlossaryValidationError(ValidatorMessage validatorMessage) {
        if (GlossaryValidationErrors == null) {
            GlossaryValidationErrors = new ArrayList<>();
        }
        GlossaryValidationErrors.add(validatorMessage);
    }

    public void addDisclaimerValidationError(ValidatorMessage validatorMessage) {
        if (DisclaimerValidationErrors == null) {
            DisclaimerValidationErrors = new ArrayList<>();
        }
        DisclaimerValidationErrors.add(validatorMessage);
    }

    public void addPdfValidationFailure(ValidatorMessage validatorMessage) {
        if (PdfValidationErrors == null) {
            PdfValidationErrors = new ArrayList<>();
        }
        PdfValidationErrors.add(validatorMessage);
    }

    public String getJson() {
        calculateResult();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        return gson.toJson(this);
    }

}
