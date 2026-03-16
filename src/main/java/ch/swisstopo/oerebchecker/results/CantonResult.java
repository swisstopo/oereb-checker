package ch.swisstopo.oerebchecker.results;

import ch.swisstopo.oerebchecker.config.models.CantonConfig;
import ch.swisstopo.oerebchecker.core.validation.MessageSeverity;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.models.Canton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import software.amazon.awssdk.utils.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CantonResult {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final String configInfo;
    private final Canton canton;

    private AvailabilityStatus availabilityStatus;
    private String availabilityExecutionDate;

    private final String executionDate;
    private final List<CheckResult> results = new ArrayList<>();

    public Canton getCanton() {
        return canton;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public String getAvailabilityExecutionDate() {
        return availabilityExecutionDate;
    }

    public String getExecutionDate() {
        return executionDate;
    }

    public List<CheckResult> getResults() {
        return results;
    }

    private static String formatDateTime(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_TIME_FORMATTER);
    }

    public CantonResult(Canton canton, CantonConfig config, LocalDateTime executionTime) {
        this.canton = canton;
        this.configInfo = config != null ? config.toString() : null;
        this.executionDate = formatDateTime(executionTime);
    }

    public void setAvailability(AvailabilityStatus status, LocalDateTime executionTime) {
        this.availabilityStatus = status;
        this.availabilityExecutionDate = formatDateTime(executionTime);
    }

    public void addCheckResult(CheckResult checkResult) {
        checkResult.calculateResult();
        results.add(checkResult);
    }

    public int getTotalCount() {
        return results.size();
    }

    public int getSuccessfulCount() {
        return (int) results.stream().filter(r -> r.Successful).count();
    }

    public int getWarningCount() {
        return results.stream().mapToInt(CheckResult::getWarningCount).sum();
    }

    public int getErrorCount() {
        return results.stream().mapToInt(CheckResult::getErrorCount).sum();
    }

    public String getAsJsonString() {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        return gson.toJson(this);
    }

    public Element getAsHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='canton-result-container'>");

        String cantonJsonFileName = (canton != null ? canton.name().toLowerCase() : "unknown") + ".json";

        html.append("<div class='meta-row'>");
        html.append("<p class='meta'>Execution Date: ").append(esc(executionDate)).append("</p>");
        html.append("<a class='meta-download' target='_blank' href='data/").append(esc(cantonJsonFileName)).append("' download>");
        html.append("Download JSON");
        html.append("</a>");
        html.append("</div>");

        if (configInfo != null && !configInfo.isBlank()) {
            html.append("<div class='config-line'>");
            html.append("<span class='config-label'>Canton-Config:</span> ");
            html.append("<code class='config-value'>").append(esc(configInfo)).append("</code>");
            html.append("</div>");
        }

        for (CheckResult result : results) {
            String statusClass = result.Successful ? "success" : "failure";
            html.append("<div class='check-card ").append(statusClass).append("'>");

            // Header: Always visible
            html.append("<div class='card-header'>");
            html.append("<span class='status-icon'>").append(result.Successful ? "✔" : "✘").append("</span>");
            html.append("<span class='url'>");
            if (StringUtils.isBlank(result.Url)) {
                html.append("config is not valid");
            } else {
                html.append(esc(result.Url)).append(result.RedirectFollowed != null && result.RedirectFollowed ? " → " + esc(result.RedirectUrl) : "");
            }
            html.append("</span>");
            html.append("<span class='badge'>HTTP ").append(result.StatusCode).append("</span>");
            html.append("</div>");

            // Content: Details and Validation Flags
            html.append("<div class='card-body'>");

            if (result.ConfigInfo != null && !result.ConfigInfo.isBlank()) {
                html.append("<div class='config-line'>");
                html.append("<span class='config-label'>Check-Config:</span> ");
                html.append("<code class='config-value'>").append(esc(result.ConfigInfo)).append("</code>");
                html.append("</div>");
            }

            html.append("<div class='validation-grid'>");
            appendFlag(html, "Status Code", result.StatusCodeCorrect);
            appendFlag(html, "Content Type", result.ContentTypeCorrect);
            appendFlag(html, "XML Schema", result.XmlIsValid);
            appendFlag(html, "PDF Valid", result.PdfIsValid);
            appendFlag(html, "Geometry", result.GeometryIsValid);
            appendFlag(html, "Languages", result.LangIsValid);
            appendFlag(html, "Topics", result.TopicsIsValid);
            appendFlag(html, "Glossary", result.GlossaryIsValid);
            appendFlag(html, "Office (UID)", result.OfficeIsValid);
            html.append("</div>");

            for (Map.Entry<String, List<ValidatorMessage>> entry : result.getValidationMessages().entrySet()) {
                String category = entry.getKey();
                List<ValidatorMessage> messages = entry.getValue();
                appendMessagesBySeverity(html, category, messages);
            }

            if (result.ErrorMessage != null) {
                appendMessagesBySeverity(html, "System Error", List.of(
                        ValidatorMessage.error(
                                "Execution",
                                "SYSTEM_ERROR",
                                "A system error occurred while executing the check.",
                                result.Url,
                                result.ErrorMessage
                        )));
            }

            html.append("</div>"); // End card-body
            html.append("</div>"); // End check-card
        }
        html.append("</div>");

        return Jsoup.parse(html.toString()).selectFirst("div.canton-result-container");
    }

    private void appendFlag(StringBuilder sb, String label, Boolean flag) {
        if (flag == null) {
            return;
        }
        String color = flag ? "#28a745" : "#dc3545";
        sb.append("<div class='flag'><span style='color:").append(color).append("'>●</span> ").append(label).append("</div>");
    }

    private void appendMessagesBySeverity(StringBuilder sb, String category, List<ValidatorMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<ValidatorMessage> errors = messages.stream()
                .filter(m -> m != null && m.Severity == MessageSeverity.ERROR)
                .toList();

        List<ValidatorMessage> warnings = messages.stream()
                .filter(m -> m != null && m.Severity == MessageSeverity.WARNING)
                .toList();

        appendDetails(sb, category + " - Errors", errors, "error-details");
        appendDetails(sb, category + " - Warnings", warnings, "warning-details");
    }

    private void appendDetails(StringBuilder sb, String title, List<ValidatorMessage> messages, String cssClass) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        sb.append("<details class='").append(cssClass).append("'>");
        sb.append("<summary>").append(esc(title)).append(" (").append(messages.size()).append(")</summary>");
        sb.append("<ul>");

        String ruleOrLabel;
        boolean hasLocation;
        boolean hasError;

        for (ValidatorMessage msg : messages) {

            ruleOrLabel = StringUtils.isNotBlank(msg.Rule) ? msg.Rule : (cssClass.equals("warning-details") ? "Warning" : "Error");
            hasLocation = StringUtils.isNotBlank(msg.Location);
            hasError = StringUtils.isNotBlank(msg.Error);

            sb.append("<li>");
            sb.append("<div class='msg-row'>");

            sb.append("<div class='msg-rule'><strong>")
                    .append(esc(ruleOrLabel))
                    .append(":</strong></div>");

            sb.append("<div class='msg-body'>")
                    .append(esc(msg.Message));

            if (hasError) {
                sb.append(" <span class='msg-error'>- ")
                        .append(esc(msg.Error))
                        .append("</span>");
            }

            if (hasLocation) {
                sb.append("<br><span class='msg-location'>@ ")
                        .append(esc(msg.Location))
                        .append("</span>");
            }

            sb.append("</div>"); // msg-body
            sb.append("</div>"); // msg-row
            sb.append("</li>");
        }
        sb.append("</ul></details>");
    }

    private static String esc(String s) {
        if (s == null) {
            return "'NULL'";
        }
        return Entities.escape(s, new Document.OutputSettings());
    }
}
