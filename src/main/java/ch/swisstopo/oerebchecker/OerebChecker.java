package ch.swisstopo.oerebchecker;

import ch.swisstopo.oerebchecker.aws.S3Storage;
import ch.swisstopo.oerebchecker.checks.GetCapabilities;
import ch.swisstopo.oerebchecker.checks.GetEGRID;
import ch.swisstopo.oerebchecker.checks.GetExtractById;
import ch.swisstopo.oerebchecker.checks.GetVersions;
import ch.swisstopo.oerebchecker.configs.Config;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.results.Result;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OerebChecker {
    protected static final Logger logger = LoggerFactory.getLogger(OerebChecker.class);

    private static final List<String> argsList = new ArrayList<>();
    private static final Map<String, String> optsList = new HashMap<>();
    private static final List<String> doubleOptsList = new ArrayList<>();

    private static final String s3AccessKeyEnvKey = "S3AccessKey";
    private static final String s3SecretKeyEnvKey = "S3SecretKey";

    private static final String s3RegionNameEnvKey = "S3RegionName";
    private static final String s3BucketEnvKey = "S3Bucket";
    private static final String s3ConfigEnvKey = "S3Config";

    private static S3Storage s3Storage = null;

    private static Path outputDirectoryPath = Path.of("").toAbsolutePath().resolve("output");

    public static void main(String[] args) {
        try {
            if (initialise(args)) {

                Config config = loadConfig();
                if (config == null) {
                    logger.error("Config could not be loaded");
                    return;
                }

                runChecks(config);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static String resultsBucketName = null;

    public static void runSingleCheck(String s3ScriptsBucket, String s3ResultsBucket, String s3Key, String s3Region) {
        // Setup S3 Storage manually
        if (s3Region == null)
            s3Region = "eu-central-1";

        // We initialize S3Storage with the scripts bucket as default, but we'll use
        // overrides
        s3Storage = new S3Storage(s3Region, s3ScriptsBucket);
        resultsBucketName = s3ResultsBucket;

        // Load Config using the key and specific bucket
        byte[] configData = s3Storage.GetBucketObject(s3ScriptsBucket, s3Key);
        if (configData == null) {
            logger.error("Could not load config from S3: {}/{}", s3ScriptsBucket, s3Key);
            return;
        }

        Config config = parseConfigData(configData, s3Key);
        if (config != null) {
            runChecks(config);
        }
    }

    private static void runChecks(Config config) {
        int threadCount = 1;
        if (config.Threads != null) {
            threadCount = config.Threads;
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {

            List<CompletableFuture<CheckResult>> completableFutures = new ArrayList<>();

            URI basicUri = URI.create(config.BasicUrl);
            if (config.OutputDirectoryPath != null) {
                outputDirectoryPath = Paths.get(config.OutputDirectoryPath).toAbsolutePath();
                logger.info("Output directory path set to: {}", outputDirectoryPath);
            } else {
                // Ensure we have a writable path for Lambda
                outputDirectoryPath = Path.of("/tmp/output");
            }

            if (config.GetVersions != null) {
                for (var checkConfig : config.GetVersions) {
                    completableFutures.add(CompletableFuture
                            .supplyAsync(() -> new GetVersions(basicUri, checkConfig).run(), executor));
                }
            }
            if (config.GetCapabilities != null) {
                for (var checkConfig : config.GetCapabilities) {
                    completableFutures.add(CompletableFuture
                            .supplyAsync(() -> new GetCapabilities(basicUri, checkConfig).run(), executor));
                }
            }
            if (config.GetEGRID != null) {
                for (var checkConfig : config.GetEGRID) {
                    completableFutures.add(
                            CompletableFuture.supplyAsync(() -> new GetEGRID(basicUri, checkConfig).run(), executor));
                }
            }
            if (config.GetExtractById != null) {
                for (var checkConfig : config.GetExtractById) {
                    completableFutures.add(CompletableFuture
                            .supplyAsync(() -> new GetExtractById(basicUri, checkConfig).run(), executor));
                }
            }

            CompletableFuture<Void> allFutures = CompletableFuture
                    .allOf(completableFutures.toArray(new CompletableFuture[0]));
            allFutures.thenRun(() -> {

                Result result = new Result();
                for (var check : completableFutures) {
                    try {
                        result.addCheckResult(check.get());
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error(e.getCause().getMessage(), e);
                    }
                }

                writeResults(result);

            }).exceptionally(throwable -> {
                logger.error(throwable.getMessage(), throwable);
                return null;
            });
            allFutures.join();

            executor.shutdown();
        }
    }

    private static boolean initialise(String[] args) {

        parseArgs(args);

        if (optsList.containsKey("-info") || optsList.containsKey("-help")) {

            logger.info("params:");
            logger.info("   -cFP -> config file path");
            // logger.info(" -oDP -> output directory path");

            return false;
        }

        return true;
    }

    private static Config loadConfig() {

        byte[] configData = null;
        String configFilePathString = null;

        if (optsList.isEmpty()) {
            StringBuilder logEntryBuilder = new StringBuilder();
            String lineSeparator = System.lineSeparator();

            String s3AccessKey = System.getenv(s3AccessKeyEnvKey);
            String s3SecretKey = System.getenv(s3SecretKeyEnvKey);
            String s3RegionName = System.getenv(s3RegionNameEnvKey);
            String s3BucketName = System.getenv(s3BucketEnvKey);
            configFilePathString = System.getenv(s3ConfigEnvKey);

            if (s3RegionName == null || s3RegionName.isBlank() ||
                    s3BucketName == null || s3BucketName.isBlank() ||
                    configFilePathString == null || configFilePathString.isBlank()) {
                logEntryBuilder.delete(0, logEntryBuilder.length());
                logEntryBuilder.append("No value for environment variable/s:");

                if (s3RegionName == null || s3RegionName.isBlank()) {
                    logEntryBuilder.append(lineSeparator);
                    logEntryBuilder.append("- " + s3RegionNameEnvKey);
                }
                if (s3BucketName == null || s3BucketName.isBlank()) {
                    logEntryBuilder.append(lineSeparator);
                    logEntryBuilder.append("- " + s3BucketEnvKey);
                }
                if (configFilePathString == null || configFilePathString.isBlank()) {
                    logEntryBuilder.append(lineSeparator);
                    logEntryBuilder.append("- " + s3ConfigEnvKey);
                }
                logger.error(logEntryBuilder.toString());

            } else {

                if (s3AccessKey == null || s3AccessKey.isBlank() ||
                        s3SecretKey == null || s3SecretKey.isBlank()) {
                    logEntryBuilder.delete(0, logEntryBuilder.length());
                    logEntryBuilder.append("No value for environment variable/s:");

                    if (s3AccessKey == null || s3AccessKey.isBlank()) {
                        logEntryBuilder.append(lineSeparator);
                        logEntryBuilder.append("- " + s3AccessKeyEnvKey);
                    }
                    if (s3SecretKey == null || s3SecretKey.isBlank()) {
                        logEntryBuilder.append(lineSeparator);
                        logEntryBuilder.append("- " + s3SecretKeyEnvKey);
                    }

                    logger.debug(logEntryBuilder.toString());
                    s3Storage = new S3Storage(s3RegionName, s3BucketName);

                } else {
                    s3Storage = new S3Storage(s3AccessKey, s3SecretKey, s3RegionName, s3BucketName);
                }

                configData = s3Storage.GetBucketObject(configFilePathString);
            }
        } else {
            for (var entry : optsList.entrySet()) {

                if (entry.getKey().equals("-cFP")) {
                    configFilePathString = entry.getValue();
                    try {
                        Path absolutePath = Paths.get(configFilePathString).toAbsolutePath();
                        configData = Files.readAllBytes(absolutePath);
                    } catch (IOException e) {
                        logger.error("Can not read config file: {}", configFilePathString, e);
                    }
                }
                /*
                 * if (entry.getKey().equals("-oDP")) {
                 * try {
                 * outputDirectoryPath = Paths.get(entry.getValue());
                 * } catch (InvalidPathException e) {
                 * logger.error("Can not read output directory path: {}", entry.getValue(), e);
                 * }
                 * }
                 */
            }
        }

        if (configData != null) {
            return parseConfigData(configData, configFilePathString);
        }

        return null;
    }

    private static Config parseConfigData(byte[] configData, String configFilePathString) {
        String configContent = new String(configData, StandardCharsets.UTF_8);
        if (configFilePathString.toLowerCase().endsWith(".json")) {
            try {
                Gson gson = new Gson();
                return gson.fromJson(configContent, Config.class);
            } catch (JsonSyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if (configFilePathString.toLowerCase().endsWith(".xml")) {
            Config config;
            try {
                StringReader sr = new StringReader(configContent);
                JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                config = (Config) unmarshaller.unmarshal(sr);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            return config;
        }
        return null;
    }

    private static void writeResults(Result result) {

        File outputDirectory = outputDirectoryPath.toFile();

        boolean outputDirectoryExists = outputDirectory.exists();
        if (!outputDirectoryExists) {
            try {
                outputDirectoryExists = outputDirectory.mkdirs();
            } catch (SecurityException ex) {
                logger.error("Can not create directory {}", outputDirectoryPath, ex);
            }
        }

        String resultJson = result.getJson();

        String resultHtmlPage = readResultHtmlTemplate();
        if (resultHtmlPage != null) {

            String resultHtmlTableRows = result.getHtmlTableRows();
            String resultHtmlCss = readResultHtmlCss();
            resultHtmlPage = resultHtmlPage.replace("#inlineCssPlaceholder",
                    resultHtmlCss != null ? resultHtmlCss : "");
            resultHtmlPage = resultHtmlPage.replace("#executionDatePlaceHolder", result.ExecutionDate);
            resultHtmlPage = resultHtmlPage.replace("#tableRowsPlaceHolder", resultHtmlTableRows);
        }

        Path outputJsonFilePath = null;
        Path outputHtmlFilePath = null;

        if (outputDirectoryExists) {
            try {
                outputJsonFilePath = outputDirectoryPath.resolve("result.json");
                FileWriter fw = new FileWriter(outputJsonFilePath.toFile());

                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(resultJson);
                bw.close();

                if (s3Storage != null) {
                    if (resultsBucketName != null) {
                        s3Storage.PutBucketObject(resultsBucketName, outputJsonFilePath);
                    } else {
                        s3Storage.PutBucketObject(outputJsonFilePath);
                    }
                }

            } catch (IOException ex) {
                logger.error("Can not write results as json to file: {}", outputJsonFilePath, ex);
            }

            try {
                outputHtmlFilePath = outputDirectoryPath.resolve("result.html");
                FileWriter fw = new FileWriter(outputHtmlFilePath.toFile());

                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(resultHtmlPage);
                bw.close();

                if (s3Storage != null) {
                    if (resultsBucketName != null) {
                        s3Storage.PutBucketObject(resultsBucketName, outputHtmlFilePath);
                    } else {
                        s3Storage.PutBucketObject(outputHtmlFilePath);
                    }
                }

            } catch (IOException ex) {
                logger.error("Can not write results as html to file: {}", outputHtmlFilePath, ex);
            }
        }

        logger.debug(resultJson);
    }

    // helper functions
    private static void parseArgs(String[] args) {

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].length() < 2) {
                    throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                }
                if (args[i].charAt(1) == '-') {
                    if (args[i].length() < 3) {
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    }
                    // --opt
                    doubleOptsList.add(args[i].substring(2));
                } else {
                    if (args.length - 1 == i) {
                        throw new IllegalArgumentException("Expected arg after: " + args[i]);
                    }
                    // -opt
                    optsList.put(args[i], args[i + 1]);
                    i++;
                }
            } else {// arg
                argsList.add(args[i]);
            }
        }
    }

    private static String readResultHtmlTemplate() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("ch/swisstopo/oerebchecker/result.html")) {
            if (is == null)
                return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            logger.error("Can not read result html template: {}", e.getMessage());
        }
        return "";
    }

    private static String readResultHtmlCss() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("ch/swisstopo/oerebchecker/result.css")) {
            if (is == null)
                return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            logger.error("Can not read result css template: {}", e.getMessage());
        }
        return "";
    }
}