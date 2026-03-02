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

    private static final String RESULT_HTML_RESOURCE = "ch/swisstopo/oerebchecker/result.html";
    private static final String TEMPLATE_VERSION_META = "oerebchecker-template-version";

    private record TemplateReadResult(Document document, boolean templateUpgraded) {
    }

    public static void write(IStorageProvider storage, Path outputDirectoryPath, CantonResult cantonResult) {
        Path outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
        Path outputJsonFilePath = outputDirectoryPath.resolve("data").resolve(cantonResult.getCanton().name().toLowerCase() + ".json");

        TemplateReadResult template = readResultHtmlTemplate(storage, outputHtmlFilePath);
        if (template == null || template.document() == null) {
            logger.error("Failed to load result.html template.");
            return;
        }

        ensureStaticAssets(storage, outputDirectoryPath, template.templateUpgraded());

        Document htmlPage = template.document();
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

        String resultJson = cantonResult.getAsJsonString();
        if (resultJson != null) {
            if (!storage.writeObject(outputJsonFilePath, new ByteArrayInputStream(resultJson.getBytes(StandardCharsets.UTF_8)))) {
                logger.error("Failed to write json file '{}'", outputJsonFilePath);
            }
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
            // Nothing in storage -> bootstrap from resources
            templateUpgraded = true;
            chosenBytes = resourceBytes;

            boolean ok = storage.writeObject(filePath, new ByteArrayInputStream(resourceBytes));
            if (!ok) {
                logger.warn("Failed to bootstrap stored result.html template at '{}'", filePath);
            }
        } else {
            String storedVersion = extractTemplateVersion(storedBytes);
            String resourceVersion = extractTemplateVersion(resourceBytes);

            boolean differs = (resourceVersion != null && !resourceVersion.equals(storedVersion))
                    || (resourceVersion == null && storedVersion != null);

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
            String baseUri = "https://example.com/";
            Document doc = Jsoup.parse(new ByteArrayInputStream(chosenBytes), StandardCharsets.UTF_8.name(), baseUri);
            return new TemplateReadResult(doc, templateUpgraded);

        } catch (IOException e) {
            logger.error("Failed to parse result template: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String extractTemplateVersion(byte[] htmlBytes) {
        try {
            Document doc = Jsoup.parse(new ByteArrayInputStream(htmlBytes), StandardCharsets.UTF_8.name(), "https://example.com/");
            Element meta = doc.selectFirst("meta[name=\"" + TEMPLATE_VERSION_META + "\"]");
            return meta != null ? meta.attr("content") : null;
        } catch (Exception e) {
            logger.debug("Failed to read template version meta", e);
            return null;
        }
    }

    private static void ensureStaticAssets(IStorageProvider storage, Path outputDirectoryPath, boolean forceOverwrite) {
        copyResourceAsset(storage,
                outputDirectoryPath.resolve("style").resolve("site.css"),
                "ch/swisstopo/oerebchecker/style/site.css",
                forceOverwrite);


        copyResourceAsset(storage,
                outputDirectoryPath.resolve("style").resolve("img").resolve("Schweiz_Eidgen_Karten_dfir_www_cmyk.jpg"),
                "ch/swisstopo/oerebchecker/style/img/Schweiz_Eidgen_Karten_dfir_www_cmyk.jpg",
                forceOverwrite);

        copyResourceAsset(storage,
                outputDirectoryPath.resolve("style").resolve("img").resolve("OEREB_Kataster_de_fr_it.jpg"),
                "ch/swisstopo/oerebchecker/style/img/OEREB_Kataster_de_fr_it.jpg",
                forceOverwrite);
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
