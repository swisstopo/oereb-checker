package ch.swisstopo.oerebchecker;

import ch.swisstopo.oerebchecker.config.models.GetVersionsConfig;
import ch.swisstopo.oerebchecker.core.checks.*;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.results.AvailabilityStatus;
import ch.swisstopo.oerebchecker.storage.IStorageProvider;
import ch.swisstopo.oerebchecker.storage.LocalStorageProvider;
import ch.swisstopo.oerebchecker.storage.S3StorageProvider;
import ch.swisstopo.oerebchecker.config.models.CantonConfig;
import ch.swisstopo.oerebchecker.config.ConfigManager;
import ch.swisstopo.oerebchecker.manager.MetadataManager;
import ch.swisstopo.oerebchecker.manager.ResultManager;
import ch.swisstopo.oerebchecker.results.CantonResult;
import ch.swisstopo.oerebchecker.results.CheckResult;
import ch.swisstopo.oerebchecker.models.Canton;
import ch.swisstopo.oerebchecker.utils.EnvVars;
import ch.swisstopo.oerebchecker.utils.RequestHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OerebChecker {
    private static final Logger logger = LoggerFactory.getLogger(OerebChecker.class);

    private static IStorageProvider storageProvider = new LocalStorageProvider();

    public static void main(String[] args) {
        try {
            execute(args);
        } catch (Exception ex) {
            logger.error("Critical failure: {}", ex.getMessage(), ex);
            // Re-throw as RuntimeException instead of System.exit() for Lambda compatibility.
            // AWS Lambda captures the exception and logs the failure correctly.
            throw new RuntimeException("Application failed", ex);
        }
    }

    public static void execute(String[] args) {
        CliParser cli = new CliParser(args);
        logger.trace("Application started with arguments: {}", String.join(" ", args));

        if (cli.isHelpRequested()) {
            cli.printHelp();
            return;
        }

        // 1. Initialize Storage
        S3StorageProvider.createFromEnv(EnvVars.S3_RESULTS_BUCKET).ifPresent(provider -> {
            storageProvider = provider;
            logger.info("S3 results storage initialized.");
        });

        // 2. Load Config
        ConfigManager configManager = new ConfigManager();
        boolean localConfigLoaded = configManager.loadConfigsFromLocal(cli);

        if (!localConfigLoaded) {
            String s3Key = cli.getKey();
            if (s3Key != null) {
                logger.debug("Local config not found, attempting to load from S3 via key: {}", s3Key);
                S3StorageProvider.createFromEnv(EnvVars.S3_SCRIPTS_BUCKET)
                        .ifPresent(provider -> configManager.loadConfigFromS3(provider, s3Key));
            } else {
                // Fallback: try loading from SCRIPTS_BUCKET with S3Config env var
                logger.debug("Local config not found and no S3 key provided, attempting fallback to SCRIPTS_BUCKET...");
                S3StorageProvider.createFromEnv(EnvVars.S3_SCRIPTS_BUCKET)
                        .ifPresent(configManager::loadConfigsFromS3);
            }
        }

        if (configManager.getCantonConfigs().isEmpty()) {
            throw new RuntimeException("Application configuration is empty or could not be parsed. Please check your config source.");
        }

        if ("availability".equals(cli.getAction())) {
            // 3. Execution Loop
            configManager.getCantonConfigs().forEach(OerebChecker::processCantonAvailability);
            logger.info("All availability checks completed.");

        } else {
            // 3. Load Metadata
            MetadataManager.loadMetadata();

            // 4. Execution Loop
            configManager.getCantonConfigs().forEach(OerebChecker::processCanton);
            logger.info("All checks completed.");
        }
    }

    private static boolean hasAvailableHttpResponse(List<CheckResult> results) {
        return results.stream()
                .filter(Objects::nonNull)
                .anyMatch(OerebChecker::hasAvailableHttpResponse);
    }

    private static boolean hasAvailableHttpResponse(CheckResult result) {
        return result != null
                && (result.StatusCode == ResponseStatusCode.OK
                || result.StatusCode == ResponseStatusCode.NO_CONTENT
                || result.StatusCode == ResponseStatusCode.SEE_OTHER
                || result.StatusCode == ResponseStatusCode.INTERNAL_SERVER_ERROR);
    }

    private static CantonResult loadExistingCantonResult(Path outputDirectoryPath, Canton canton) {
        Path outputJsonFilePath = ResultManager.getOutputJsonFilePath(outputDirectoryPath, canton);

        if (!storageProvider.exists(outputJsonFilePath)) {
            return null;
        }

        byte[] existingJson = storageProvider.readObject(outputJsonFilePath);
        if (existingJson == null || existingJson.length == 0) {
            return null;
        }

        try {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(new String(existingJson, StandardCharsets.UTF_8), CantonResult.class);
        } catch (Exception e) {
            logger.warn("Failed to deserialize existing canton result '{}': {}", outputJsonFilePath, e.getMessage());
            return null;
        }
    }

    private static void processCantonAvailability(Canton canton, CantonConfig config) {
        logger.trace("Processing availability for Canton: {} (cantonConfig={})", canton, config);

        if (StringUtils.isNotBlank(config.ProxyHostname)) {
            RequestHelper.setProxySettings(config.ProxyHostname, config.ProxyPort, config.ProxyUser, config.ProxyPassword);
        }

        int threadCount = config.Threads != null ? config.Threads : 1;

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            URI baseUri = URI.create(config.BasicUrl);

            GetVersionsConfig getVersionsConfig = new GetVersionsConfig();
            getVersionsConfig.ExpectedStatusCode = ResponseStatusCode.OK;
            getVersionsConfig.FORMAT = ResponseFormat.xml.name();
            getVersionsConfig.FollowOneRedirect = true;

            ICheck availabilityCheck = new GetVersions(baseUri, getVersionsConfig);
            logger.trace("Adding GetVersions as availability task for Canton {} with config: {}", canton, getVersionsConfig);

            String cantonName = canton.name();
            CompletableFuture<CheckResult> future = CompletableFuture.supplyAsync(() -> {
                MDC.put("canton", cantonName);
                try {
                    return availabilityCheck.run();
                } finally {
                    MDC.remove("canton");
                }
            }, executor);

            CheckResult checkResult;
            try {
                checkResult = future.get();
            } catch (Exception e) {
                logger.error("Availability check failed", e);
                checkResult = null;
            }

            AvailabilityStatus status = hasAvailableHttpResponse(checkResult)
                    ? AvailabilityStatus.AVAILABLE
                    : AvailabilityStatus.UNAVAILABLE;

            logger.info("Canton {} availability result: {} (httpStatusCode={})",
                    canton,
                    status,
                    checkResult != null ? checkResult.StatusCode : null
            );

            Path outputDirectoryPath = getOutputDirectoryPath(config);
            CantonResult cantonResult = loadExistingCantonResult(outputDirectoryPath, canton);

            if (cantonResult == null) {
                cantonResult = new CantonResult(canton, config, null);
            }

            cantonResult.setAvailability(status, LocalDateTime.now());

            logger.info("Finalizing Canton {} availability. Writing results to '{}' via provider {}",
                    canton,
                    ResultManager.getOutputJsonFilePath(outputDirectoryPath, canton),
                    storageProvider.getClass().getSimpleName()
            );

            ResultManager.write(
                    storageProvider,
                    outputDirectoryPath,
                    cantonResult
            );
        }
    }

    private static void processCanton(Canton canton, CantonConfig config) {

        logger.trace("Processing Canton {} (cantonConfig={})", canton, config.toString());

        // Proxy Settings
        if (StringUtils.isNotBlank(config.ProxyHostname)) {
            RequestHelper.setProxySettings(config.ProxyHostname, config.ProxyPort, config.ProxyUser, config.ProxyPassword);
        }

        int threadCount = config.Threads != null ? config.Threads : 1;

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            URI baseUri = URI.create(config.BasicUrl);
            List<ICheck> taskList = new ArrayList<>();

            if (config.GetVersions != null) {
                config.GetVersions.forEach(c -> {
                    logger.trace("Adding GetVersions task for Canton {} with config: {}", canton, c);
                    taskList.add(new GetVersions(baseUri, c));
                });
            }
            if (config.GetCapabilities != null) {
                config.GetCapabilities.forEach(c -> {
                    logger.trace("Adding GetCapabilities task for Canton {} with config: {}", canton, c);
                    taskList.add(new GetCapabilities(baseUri, c));
                });
            }
            if (config.GetEGRID != null) {
                config.GetEGRID.forEach(c -> {
                    logger.trace("Adding GetEGRID task for Canton {} with config: {}", canton, c);
                    taskList.add(new GetEGRID(baseUri, c));
                });
            }
            if (config.GetExtractById != null) {
                config.GetExtractById.stream()
                        .flatMap(c -> c.getPossibleConfigs().stream())
                        .forEach(pc -> {
                            logger.trace("Adding GetExtractById task for Canton {} with config: {}", canton, pc);
                            taskList.add(new GetExtractById(baseUri, pc));
                        });
            }

            logger.trace("Submitting {} tasks to executor for Canton {}", taskList.size(), canton);

            String cantonName = canton.name();
            List<CompletableFuture<CheckResult>> futures = taskList.stream()
                    .map(check -> CompletableFuture.supplyAsync(() -> {
                        // Set context for the worker thread
                        MDC.put("canton", cantonName);
                        try {
                            return check.run();
                        } finally {
                            MDC.remove("canton");
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            LocalDateTime executionTime = LocalDateTime.now();
            CantonResult cantonResult = new CantonResult(canton, config, executionTime);

            futures.forEach(f -> {
                try {
                    cantonResult.addCheckResult(f.get());
                } catch (Exception e) {
                    logger.error("Check failed", e);
                }
            });

            AvailabilityStatus status = hasAvailableHttpResponse(cantonResult.getResults())
                    ? AvailabilityStatus.AVAILABLE
                    : AvailabilityStatus.UNAVAILABLE;

            cantonResult.setAvailability(status, executionTime);

            Map<CheckStatus, Long> byStatus = cantonResult.getResults().stream()
                    .collect(Collectors.groupingBy(
                            r -> r.ExecutionStatus != null ? r.ExecutionStatus : CheckStatus.NOT_STARTED,
                            Collectors.counting()
                    ));

            long executed = byStatus.getOrDefault(CheckStatus.EXECUTED, 0L);
            long skipped = byStatus.getOrDefault(CheckStatus.SKIPPED, 0L);
            long failed = byStatus.getOrDefault(CheckStatus.FAILED, 0L);

            logger.info("Canton {} summary: total={}, executed={}, successful={}, warnings={}, skipped={}, failed={}",
                    canton,
                    cantonResult.getResults().size(),
                    executed,
                    cantonResult.getSuccessfulCount(),
                    cantonResult.getWarningCount(),
                    skipped,
                    failed
            );

            Path outputDirectoryPath = getOutputDirectoryPath(config);
            logger.info("Finalizing Canton {}. Writing results to '{}' via provider {}",
                    canton,
                    outputDirectoryPath,
                    storageProvider.getClass().getSimpleName()
            );

            ResultManager.write(storageProvider, outputDirectoryPath, cantonResult);
        }
    }

    private static Path getOutputDirectoryPath(CantonConfig config) {

        // Use null check instead of isNotBlank because an empty string ("") represents the S3 bucket root.
        String envPath = System.getenv(EnvVars.S3_RESULTS_OUTPUT_PATH);
        if (envPath != null) {
            logger.trace("Found & use {}: '{}'", EnvVars.S3_RESULTS_OUTPUT_PATH, envPath);
            return Paths.get(envPath);
        }
        envPath = System.getenv(EnvVars.OUTPUT_PATH);
        if (StringUtils.isNotBlank(envPath)) {
            logger.trace("Found & use {}: '{}'", EnvVars.OUTPUT_PATH, envPath);
            return Paths.get(envPath);
        }
        if (StringUtils.isNotBlank(config.OutputDirectoryPath)) {
            logger.trace("Found & use config OutputDirectoryPath: '{}'", config.OutputDirectoryPath);
            return Paths.get(config.OutputDirectoryPath);
        }

        // Default: bucket root (empty path for S3, or current directory for local).
        logger.trace("Using default output path: root");
        return Paths.get("");
    }
}