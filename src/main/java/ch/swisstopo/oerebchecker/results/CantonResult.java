package ch.swisstopo.oerebchecker.results;

import ch.swisstopo.oerebchecker.models.Canton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CantonResult {

    private final Canton canton;
    private final String executionDate;
    private final List<CheckResult> results = new ArrayList<>();

    public Canton getCanton() {
        return canton;
    }

    public String getExecutionDate() {
        return executionDate;
    }

    public List<CheckResult> getResults() {
        return results;
    }

    public CantonResult(Canton canton) {
        this.canton = canton;
        executionDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); // DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
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

        StringBuilder htmlTableRows = new StringBuilder();

        htmlTableRows.append("<table class=\"responsive-table\">");
        htmlTableRows.append("<thead>");
        htmlTableRows.append("<tr>");
        htmlTableRows.append("<th scope=\"col\">Url</th>");
        htmlTableRows.append("<th scope=\"col\">StatusCode</th>");
        htmlTableRows.append("<th scope=\"col\">ContentType</th>");
        htmlTableRows.append("<th scope=\"col\">XmlIsValid</th>");
        htmlTableRows.append("<th scope=\"col\">PdfIsValid</th>");
        htmlTableRows.append("<th scope=\"col\">Check successful</th>");
        htmlTableRows.append("</tr>");
        htmlTableRows.append("</thead>");
        htmlTableRows.append("<tfoot>");
        htmlTableRows.append("<tr>");
        htmlTableRows.append("<td colspan=\"6\">Execution Date: ").append(executionDate).append("</td>");
        htmlTableRows.append("</tr>");
        htmlTableRows.append("</tfoot>");
        htmlTableRows.append("<tbody>");

        String colClass;
        for (CheckResult result : results) {
            htmlTableRows.append("<tr>");
            htmlTableRows.append("<th scope=\"row\">").append(result.Url).append("</td>");

            colClass = result.StatusCodeCorrect ? "green" : "red";
            htmlTableRows.append("<td class=\"").append(colClass).append("\">").append(result.StatusCode).append("</td>");

            colClass = result.ContentTypeCorrect == null ? "" : (result.ContentTypeCorrect ? "green" : "red");
            htmlTableRows.append("<td class=\"").append(colClass).append("\">").append(result.ContentType == null ? "-" : result.ContentType).append("</td>");

            colClass = result.XmlIsValid == null ? "" : (result.XmlIsValid ? "green" : "red");
            htmlTableRows.append("<td class=\"").append(colClass).append("\">").append(result.XmlIsValid == null ? "-" : result.XmlIsValid).append("</td>");

            colClass = result.PdfIsValid == null ? "" : (result.PdfIsValid ? "green" : "red");
            htmlTableRows.append("<td class=\"").append(colClass).append("\">").append(result.PdfIsValid == null ? "-" : result.PdfIsValid).append("</td>");

            colClass = result.Successful ? "green" : "red";
            htmlTableRows.append("<td class=\"").append(colClass).append("\">").append(result.Successful).append("</td>");
            htmlTableRows.append("</tr>");
        }

        htmlTableRows.append("</tbody>");
        htmlTableRows.append("</table>");

        return Jsoup.parse(htmlTableRows.toString()).selectFirst("table");
    }
}
