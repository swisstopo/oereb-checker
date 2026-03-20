package ch.swisstopo.oerebchecker.results;

import ch.swisstopo.oerebchecker.core.checks.CheckStatus;
import ch.swisstopo.oerebchecker.core.validation.MessageSeverity;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CheckResult {

    public String ConfigInfo;

    public CheckStatus ExecutionStatus = CheckStatus.NOT_STARTED;
    public String NotExecutedReasonCode = null;
    public String NotExecutedReason = null;

    public String ExceptionType = null;
    public String ExceptionMessage = null;

    public String Url;
    public String RedirectUrl;
    public Boolean RedirectFollowed = null;

    public int StatusCode;
    public boolean StatusCodeCorrect = false;

    public String ContentType;
    public Boolean ContentTypeCorrect = null;

    public Boolean XmlIsValid = null;
    public Boolean PdfIsValid = null;
    public Boolean SupportedVersionIsValid = null;
    public Boolean RealEstate_DPRIsValid = null;
    public Boolean RefsAreValid = null;
    public Boolean GeometryIsValid = null;
    public Boolean LangIsValid = null;
    public Boolean TopicsIsValid = null;
    public Boolean FederalTopicLawDocumentsIsValid = null;
    public Boolean RestrictionOnLandownershipIsValid = null;
    public Boolean ImageAspectRatioIsValid = null;
    public Boolean GlossaryIsValid = null;
    public Boolean DisclaimerIsValid = null;
    public Boolean OfficeIsValid = null;
    public Boolean WithImagesIsValid = null;

    public boolean Successful = false;
    public Boolean HasError = null;
    public String ErrorMessage = null;

    @SerializedName("ValidationMessages")
    private final Map<String, List<ValidatorMessage>> validationMessages = new LinkedHashMap<>();

    public CheckResult() {
    }

    public void setUrl(URI uri) {
        if(uri != null) {
            Url = uri.toString();
        }
    }

    public void setRedirectUrl(URI uri) {
        if(uri != null) {
            RedirectUrl = uri.toString();
        }
    }

    public Map<String, List<ValidatorMessage>> getValidationMessages() {
        return validationMessages;
    }

    public void addMessage(String category, ValidatorMessage message) {
        validationMessages.computeIfAbsent(category, k -> new ArrayList<>()).add(message);
    }

    public int getWarningCount() {
        return validationMessages.values().stream()
                .flatMap(List::stream)
                .filter(m -> m != null && m.Severity == MessageSeverity.WARNING)
                .mapToInt(m -> 1)
                .sum();
    }

    public int getErrorCount() {
        return validationMessages.values().stream()
                .flatMap(List::stream)
                .filter(m -> m != null && m.Severity == MessageSeverity.ERROR)
                .mapToInt(m -> 1)
                .sum();
    }

    public void calculateResult() {

        if (ExecutionStatus != CheckStatus.EXECUTED) {
            Successful = false;
            return;
        }

        Successful = StatusCodeCorrect
                && isOk(ContentTypeCorrect)
                && isOk(XmlIsValid)
                && isOk(PdfIsValid)
                && isOk(SupportedVersionIsValid)
                && isOk(RealEstate_DPRIsValid)
                && isOk(RefsAreValid)
                && isOk(GeometryIsValid)
                && isOk(LangIsValid)
                && isOk(TopicsIsValid)
                && isOk(FederalTopicLawDocumentsIsValid)
                && isOk(RestrictionOnLandownershipIsValid)
                && isOk(ImageAspectRatioIsValid)
                && isOk(GlossaryIsValid)
                && isOk(DisclaimerIsValid)
                && isOk(OfficeIsValid)
                && isOk(WithImagesIsValid)
                && isOk(HasError == null || !HasError);
    }

    private boolean isOk(Boolean validationFlag) {
        return validationFlag == null || validationFlag;
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
