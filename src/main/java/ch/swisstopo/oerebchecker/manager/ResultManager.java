package ch.swisstopo.oerebchecker.manager;

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
            Element resultPlaceholder = htmlPage.selectFirst("div#" + cantonResult.getCanton().name().toLowerCase() + "-checkResultPlaceholder");
            if (resultPlaceholder != null) {
                resultPlaceholder.replaceWith(cantonResult.getAsHtml());
            }

            byte[] htmlBytes = htmlPage.html().getBytes(StandardCharsets.UTF_8);
            storage.writeObject(outputHtmlFilePath, new ByteArrayInputStream(htmlBytes));
        }

        String resultJson = cantonResult.getAsJsonString();
        if (resultJson != null) {
            storage.writeObject(outputJsonFilePath, new ByteArrayInputStream(resultJson.getBytes(StandardCharsets.UTF_8)));
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
