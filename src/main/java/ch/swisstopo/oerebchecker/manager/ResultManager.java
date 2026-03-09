package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oerebchecker.models.Canton;
import ch.swisstopo.oerebchecker.storage.IStorageProvider;
import ch.swisstopo.oerebchecker.results.CantonResult;
import ch.swisstopo.oerebchecker.utils.EnvVars;
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

    private static final String RESULT_HTML_RESOURCE = "ch/swisstopo/oerebchecker/result.html";
    private static final String TEMPLATE_VERSION_META = "oerebchecker-template-version";

    private record TemplateReadResult(Document document, boolean templateUpgraded) {
    }

    public static void write(IStorageProvider storage, Path outputDirectoryPath, CantonResult cantonResult) {

        // Skip HTML writing when running inside AWS Lambda.
        // Multiple canton lambdas run in parallel and would race on the single shared
        // result.html in S3 (read-modify-write). The HTML is built once afterwards by
        // the dedicated aggregate() step instead.
        boolean inLambda = System.getenv(EnvVars.AWS_LAMBDA_FUNCTION_NAME) != null;
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
     * Called once after all canton lambdas have finished.
     */
    public static void aggregate(IStorageProvider storage, Path outputDirectoryPath) {
        Gson gson = new GsonBuilder().create();
        String jsonPrefix = outputDirectoryPath.toString().isEmpty() ? "data/" : outputDirectoryPath + "/data/";

        List<Path> jsonFiles = storage.listObjects(jsonPrefix);
        if (jsonFiles.isEmpty()) {
            logger.warn("No canton JSON files found under prefix '{}', skipping HTML generation", jsonPrefix);
            return;
        }

        Path outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
        TemplateReadResult template = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (template == null || template.document() == null) {
            logger.error("Could not load HTML template, aborting aggregation");
            return;
        }

        ensureStaticAssets(storage, outputDirectoryPath, template.templateUpgraded());

        Document htmlPage = template.document();
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
                logger.error("Failed to deserialize canton result from {}: {}", jsonPath, e.getMessage(), e);
            }
        }

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

        TemplateReadResult template = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (template == null || template.document() == null) {
            logger.error("Failed to load result.html template.");
            return;
        }

        ensureStaticAssets(storage, outputDirectoryPath, template.templateUpgraded());

        Document htmlPage = template.document();
        writeHtml(htmlPage, cantonResult);

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

    private static void writeHtml(Document htmlPage, CantonResult cantonResult) {
        clearCanton(htmlPage, cantonResult.getCanton());

        String placeholderId = getCantonPlaceholderId(cantonResult.getCanton());
        Element resultPlaceholder = htmlPage.getElementById(placeholderId);

        if (resultPlaceholder != null) {
            int total = cantonResult.getTotalCount();
            int successful = cantonResult.getSuccessfulCount();
            int warningCount = cantonResult.getWarningCount();
            int errorCount = cantonResult.getErrorCount();

            String statusClass;
            if (successful != total || errorCount > 0) {
                statusClass = "has-failures";
            } else if (warningCount > 0) {
                statusClass = "has-warnings";
            } else {
                statusClass = "all-success";
            }

            Element details = new Element("details");
            details.id(placeholderId);
            details.addClass("canton-section").addClass(statusClass);

            Element summary = new Element("summary");
            summary.append("<span class='canton-title'>Canton: " + cantonResult.getCanton() + "</span>");

            String stats = "";
            if (warningCount > 0) {
                stats += "Warnings: " + warningCount;
            }
            if (errorCount > 0) {
                if (!stats.isEmpty()) {
                    stats += " | ";
                }
                stats += "Errors: " + errorCount;
            }
            if (!stats.isEmpty()) {
                stats += " | ";
            }
            stats += "Checks " + successful + " / " + total + " successful";

            summary.append("<span class='canton-stats'>" + stats + "</span>");

            details.appendChild(summary);
            details.appendChild(cantonResult.getAsHtml());

            resultPlaceholder.replaceWith(details);
        }
    }

    private static TemplateReadResult readResultHtmlTemplate(IStorageProvider storage, Path filePath) {
        byte[] resourceBytes;
        try {
            resourceBytes = ResourceHelper.readResourceAsBytes(RESULT_HTML_RESOURCE);
        } catch (IOException e) {
            logger.error("Failed to read HTML result template from resources: {}", e.getMessage(), e);
            return null;
        }

        if (resourceBytes.length == 0) {
            logger.error("Result HTML template missing/empty in resources: {}", RESULT_HTML_RESOURCE);
            return null;
        }

        byte[] chosenBytes;
        boolean templateUpgraded = false;

        byte[] storedBytes = null;
        if (storage.exists(filePath)) {
            storedBytes = storage.readObject(filePath);
        }

        if (storedBytes == null || storedBytes.length == 0) {
            templateUpgraded = true;
            chosenBytes = resourceBytes;

            boolean ok = storage.writeObject(filePath, new ByteArrayInputStream(resourceBytes));
            if (!ok) {
                logger.warn("Failed to bootstrap stored result.html template at '{}'", filePath);
            }
        } else {
            String storedVersion = extractTemplateVersion(storedBytes);
            String resourceVersion = extractTemplateVersion(resourceBytes);

            boolean differs = (resourceVersion != null && !resourceVersion.equals(storedVersion)) || (resourceVersion == null && storedVersion != null);
            if (differs) {
                logger.info("Updating stored result.html template (storedVersion={}, resourceVersion={})", storedVersion, resourceVersion);
                templateUpgraded = true;
                chosenBytes = resourceBytes;

                boolean ok = storage.writeObject(filePath, new ByteArrayInputStream(resourceBytes));
                if (!ok) {
                    logger.warn("Failed to update stored result.html template at '{}'", filePath);
                }
            } else {
                chosenBytes = storedBytes;
            }
        }

        try {
            Document doc = Jsoup.parse(
                new ByteArrayInputStream(chosenBytes),
                StandardCharsets.UTF_8.name(),
                "https://example.com/"
            );
            return new TemplateReadResult(doc, templateUpgraded);
        } catch (IOException e) {
            logger.error("Failed to parse result template: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String extractTemplateVersion(byte[] htmlBytes) {
        try {
            Document doc = Jsoup.parse(
                new ByteArrayInputStream(htmlBytes),
                StandardCharsets.UTF_8.name(),
                "https://example.com/"
            );
            Element meta = doc.selectFirst("meta[name=\"" + TEMPLATE_VERSION_META + "\"]");
            return meta != null ? meta.attr("content") : null;
        } catch (Exception e) {
            logger.debug("Failed to read template version meta", e);
            return null;
        }
    }

    private static void ensureStaticAssets(IStorageProvider storage, Path outputDirectoryPath, boolean forceOverwrite) {
        copyResourceAsset(
                storage,
                outputDirectoryPath.resolve("style").resolve("site.css"),
                "ch/swisstopo/oerebchecker/style/site.css",
                forceOverwrite
        );

        copyResourceAsset(
                storage,
                outputDirectoryPath.resolve("style").resolve("img").resolve("Schweiz_Eidgen_Karten_dfir_www_cmyk.jpg"),
                "ch/swisstopo/oerebchecker/style/img/Schweiz_Eidgen_Karten_dfir_www_cmyk.jpg",
                forceOverwrite
        );

        copyResourceAsset(
                storage,
                outputDirectoryPath.resolve("style").resolve("img").resolve("OEREB_Kataster_de_fr_it.jpg"),
                "ch/swisstopo/oerebchecker/style/img/OEREB_Kataster_de_fr_it.jpg",
                forceOverwrite
        );
    }

    private static void copyResourceAsset(IStorageProvider storage, Path targetPath, String classpathResourcePath, boolean forceOverwrite) {
        try {
            if (!forceOverwrite && storage.exists(targetPath)) {
                return;
            }

            byte[] bytes = ResourceHelper.readResourceAsBytes(classpathResourcePath);
            if (bytes.length == 0) {
                logger.warn("Static asset not found or empty: {}", classpathResourcePath);
                return;
            }

            boolean ok = storage.writeObject(targetPath, new ByteArrayInputStream(bytes));
            if (!ok) {
                logger.error("Failed to write static asset to '{}'", targetPath);
            }
        } catch (Exception e) {
            logger.error("Failed to copy static asset '{}' -> '{}'", classpathResourcePath, targetPath, e);
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
}
