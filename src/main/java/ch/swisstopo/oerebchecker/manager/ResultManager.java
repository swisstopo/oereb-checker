package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oerebchecker.models.Canton;
import ch.swisstopo.oerebchecker.storage.IStorageProvider;
import ch.swisstopo.oerebchecker.results.CantonResult;
import ch.swisstopo.oerebchecker.utils.ResourceHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ResultManager {
    private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

    public static void write(IStorageProvider storage, Path outputDirectoryPath, CantonResult cantonResult) {
        Path outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
        Path outputJsonFilePath = outputDirectoryPath.resolve("data").resolve(cantonResult.getCanton().name().toLowerCase() + ".json");

        Document htmlPage = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (htmlPage != null) {
            clearCanton(htmlPage, cantonResult.getCanton());

            String placeholderId = getCantonPlaceholderId(cantonResult.getCanton());
            Element resultPlaceholder = htmlPage.getElementById(placeholderId);

            if (resultPlaceholder != null) {
                long total = cantonResult.getResults().size();
                long successful = cantonResult.getResults().stream().filter(r -> r.Successful).count();
                String statusClass = (successful == total) ? "all-success" : "has-failures";

                Element details = new Element("details");
                details.id(placeholderId);
                details.addClass("canton-section").addClass(statusClass);

                Element summary = new Element("summary");
                summary.append("<span class='canton-title'>Canton: " + cantonResult.getCanton() + "</span>");
                summary.append("<span class='canton-stats'>" + successful + " / " + total + " successful</span>");

                details.appendChild(summary);
                details.appendChild(cantonResult.getAsHtml());

                resultPlaceholder.replaceWith(details);
            }

            // Update footer year dynamically
            Element footerPara = htmlPage.selectFirst("footer p");
            if (footerPara != null) {
                int currentYear = java.time.LocalDate.now().getYear();
                footerPara.text("© " + currentYear + " Swisstopo - OeREB Checker");
            }

            byte[] htmlBytes = htmlPage.outerHtml().getBytes(StandardCharsets.UTF_8);
            if (!storage.writeObject(outputHtmlFilePath, new ByteArrayInputStream(htmlBytes))) {
                logger.error("Failed to write html file '{}'", outputHtmlFilePath);
            }
        }

        String resultJson = cantonResult.getAsJsonString();
        if (resultJson != null) {
            if (!storage.writeObject(outputJsonFilePath, new ByteArrayInputStream(resultJson.getBytes(StandardCharsets.UTF_8)))) {
                logger.error("Failed to write json file '{}'", outputJsonFilePath);
            }
        }
    }

    private static String getCantonPlaceholderId(Canton canton) {
        return canton.name().toLowerCase() + "-checkResultPlaceholder";
    }

    private static void clearCanton(Document doc, Canton canton) {

        String placeholderId = getCantonPlaceholderId(canton);
        Element existingSection = doc.getElementById(placeholderId);

        if (existingSection != null) {
            Element pendingDiv = new Element("div");
            pendingDiv.id(placeholderId);
            pendingDiv.addClass("canton-section").addClass("disabled");
            pendingDiv.append("<div class='summary-mock'><span class='canton-title'>Canton: " + canton + "</span><span class='canton-stats'>Pending...</span></div>");

            existingSection.replaceWith(pendingDiv);
        }
    }


    private static Document readResultHtmlTemplate(IStorageProvider storage, Path filePath) {
        byte[] data = null;

        if (storage.exists(filePath)) {
            data = storage.readObject(filePath);
        }
        // If not in storage, fall back to resources
        if (data == null) {
            try {
                data = ResourceHelper.readResourceAsBytes("ch/swisstopo/oerebchecker/result.html");
            } catch (IOException e) {
                logger.error("Failed to read HTML result template: {}", e.getMessage(), e);
            }
        }

        if (data == null) {
            return null;
        }

        try {
            String baseUri = "https://example.com/";
            Document doc = Jsoup.parse(new ByteArrayInputStream(data), StandardCharsets.UTF_8.name(), baseUri);

            String resultHtmlCss = readResultHtmlCss();
            if (resultHtmlCss != null) {
                Element style = doc.selectFirst("head>style");
                if (style != null) {
                    style.text(resultHtmlCss);
                }
            }
            return doc;

        } catch (IOException e) {
            logger.error("Failed to parse result template: {}", e.getMessage());
            return null;
        }
    }

    private static String readResultHtmlCss() {
        try {
            return ResourceHelper.readResourceAsString("ch/swisstopo/oerebchecker/result.css");
        } catch (Exception e) {
            logger.error("Failed to read CSS result template: {}", e.getMessage(), e);
        }
        return null;
    }
}
