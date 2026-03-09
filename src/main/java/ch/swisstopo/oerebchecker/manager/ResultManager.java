package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oerebchecker.models.Canton;
import ch.swisstopo.oerebchecker.storage.IStorageProvider;
import ch.swisstopo.oerebchecker.results.CantonResult;
import ch.swisstopo.oerebchecker.utils.ResourceHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class ResultManager {
    private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

    public static void write(IStorageProvider storage, Path outputDirectoryPath, CantonResult cantonResult) {
        // Skip HTML writing when running inside AWS Lambda.
        // Multiple canton lambdas run in parallel and would race on the single shared
        // result.html in S3 (read-modify-write). The HTML is built once afterwards by
        // the dedicated aggregate() step in the Step Function instead.
        boolean inLambda = System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;
        if (!inLambda) {
            writeHtml(storage, outputDirectoryPath, cantonResult);
        }

        Path outputJsonFilePath = outputDirectoryPath.resolve("data").resolve(cantonResult.getCanton().name().toLowerCase() + ".json");
        String resultJson = cantonResult.getAsJsonString();
        if (resultJson != null) {
            if (!storage.writeObject(outputJsonFilePath, new ByteArrayInputStream(resultJson.getBytes(StandardCharsets.UTF_8)))) {
                logger.error("Failed to write json file '{}'", outputJsonFilePath);
            }
        }
    }

    /**
     * Reads all per-canton JSON files from storage and builds a single result.html.
     * Called once after all canton lambdas have finished (Step Function aggregate step).
     */
    public static void aggregate(IStorageProvider storage, Path outputDirectoryPath) {
        Gson gson = new GsonBuilder().create();
        String jsonPrefix = outputDirectoryPath.toString().isEmpty()
                ? "data/"
                : outputDirectoryPath + "/data/";

        List<Path> jsonFiles = storage.listObjects(jsonPrefix);
        if (jsonFiles.isEmpty()) {
            logger.warn("No canton JSON files found under prefix '{}', skipping HTML generation", jsonPrefix);
            return;
        }

        Path outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
        Document htmlPage = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (htmlPage == null) {
            logger.error("Could not load HTML template, aborting aggregation");
            return;
        }

        for (Path jsonPath : jsonFiles) {
            byte[] data = storage.readObject(jsonPath);
            if (data == null) {
                logger.warn("Could not read {}", jsonPath);
                continue;
            }
            try {
                CantonResult cantonResult = gson.fromJson(new String(data, StandardCharsets.UTF_8), CantonResult.class);
                writeHtml(htmlPage, cantonResult);
            } catch (Exception e) {
                logger.error("Failed to deserialize canton result from {}: {}", jsonPath, e.getMessage());
            }
        }

        // Update footer year dynamically
        Element footerPara = htmlPage.selectFirst("footer p");
        if (footerPara != null) {
            int currentYear = java.time.LocalDate.now().getYear();
            footerPara.text("© " + currentYear + " Swisstopo - OeREB Checker");
        }

        byte[] htmlBytes = htmlPage.outerHtml().getBytes(StandardCharsets.UTF_8);
        if (!storage.writeObject(outputHtmlFilePath, new ByteArrayInputStream(htmlBytes))) {
            logger.error("Failed to write aggregated result.html");
        } else {
            logger.info("Aggregated result.html written successfully ({} cantons)", jsonFiles.size());
        }
    }

    private static void writeHtml(IStorageProvider storage, Path outputDirectoryPath, CantonResult cantonResult) {
        Path outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
        Document htmlPage = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (htmlPage != null) {
            writeHtml(htmlPage, cantonResult);
            byte[] htmlBytes = htmlPage.outerHtml().getBytes(StandardCharsets.UTF_8);
            if (!storage.writeObject(outputHtmlFilePath, new ByteArrayInputStream(htmlBytes))) {
                logger.error("Failed to write html file '{}'", outputHtmlFilePath);
            }
        }
    }

    private static void writeHtml(Document htmlPage, CantonResult cantonResult) {
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
