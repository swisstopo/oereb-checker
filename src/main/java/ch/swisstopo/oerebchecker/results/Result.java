package ch.swisstopo.oerebchecker.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Result {

    public String ExecutionDate;
    public List<CheckResult> Results = new ArrayList<>();

    public Result() {
        ExecutionDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); // DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public void addCheckResult(CheckResult checkResult) {
        checkResult.calculateResult();
        Results.add(checkResult);
    }

    public String getJson() {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        return gson.toJson(this);
    }

    public String getHtmlTableRows() {
        StringBuilder htmlTableRows = new StringBuilder();

        String colClass;
        for (CheckResult result : Results) {
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

        return htmlTableRows.toString();
    }
}
