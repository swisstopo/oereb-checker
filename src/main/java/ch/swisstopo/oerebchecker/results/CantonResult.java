package ch.swisstopo.oerebchecker.results;

import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.models.Canton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import software.amazon.awssdk.utils.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CantonResult {

    private final Canton canton;
    private final String executionDate;
    private final List<CheckResult> results = new ArrayList<>();

    public Canton getCanton() {
        return canton;
    }

    public List<CheckResult> getResults() {
        return results;
    }

    public CantonResult(Canton canton) {
        this.canton = canton;
        executionDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public void addCheckResult(CheckResult checkResult) {
        checkResult.calculateResult();
        results.add(checkResult);
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
        // html.append("<h2>Canton: ").append(canton).append("</h2>");
        html.append("<p class='meta'>Execution Date: ").append(executionDate).append("</p>");

        for (CheckResult result : results) {
            String statusClass = result.Successful ? "success" : "failure";
            html.append("<div class='check-card ").append(statusClass).append("'>");

            // Header: Always visible
            html.append("<div class='card-header'>");
            html.append("<span class='status-icon'>").append(result.Successful ? "✔" : "✘").append("</span>");
            html.append("<span class='url'>").append(result.Url).append("</span>");
            html.append("<span class='badge'>HTTP ").append(result.StatusCode).append("</span>");
            html.append("</div>");

            // Content: Details and Validation Flags
            html.append("<div class='card-body'>");
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
                appendErrorDetails(html, category, messages);
            }

            if (result.ErrorMessage != null) {
                appendErrorDetails(html, "System Error", List.of(new ValidatorMessage("Critical", result.ErrorMessage)));
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

    private void appendErrorDetails(StringBuilder sb, String title, List<ValidatorMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        sb.append("<details class='error-details'>");
        sb.append("<summary>").append(title).append(" (").append(messages.size()).append(")</summary>");
        sb.append("<ul>");
        for (ValidatorMessage msg : messages) {
            sb
                    .append("<li><strong>")
                    .append(StringUtils.isNotBlank(msg.Rule) ? msg.Rule : "Error")
                    .append(":</strong> ")
                    .append(msg.Message).append(StringUtils.isNotBlank(msg.Error) ? (" - " + msg.Error) : "")
                    .append("</li>");
        }
        sb.append("</ul></details>");
    }
}
