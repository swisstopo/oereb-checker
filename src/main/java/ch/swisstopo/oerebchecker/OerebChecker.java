package ch.swisstopo.oerebchecker;

import ch.swisstopo.oerebchecker.core.checks.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        // 3. Load Metadata
        MetadataManager.loadMetadata();

        // 4. Execution Loop
        configManager.getCantonConfigs().forEach(OerebChecker::processCanton);

        logger.info("All checks completed.");
    }

    private static void processCanton(Canton canton, CantonConfig config) {

        logger.trace("Processing Canton: {} (cantonConfig={})", canton, config.toString());

        // Proxy Settings
        if (StringUtils.isNotBlank(config.ProxyHostname)) {
            RequestHelper.setProxySettings(config.ProxyHostname, config.ProxyPort, config.ProxyUser, config.ProxyPassword);
        }

        int threadCount = 1;
        if (config.Threads != null) {
            threadCount = config.Threads;
        }

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

            CantonResult result = new CantonResult(canton, config.toString());

            futures.forEach(f -> {
                try {
                    result.addCheckResult(f.get());
                } catch (Exception e) {
                    logger.error("Check failed", e);
                }
            });

            Map<CheckStatus, Long> byStatus = result.getResults().stream()
                    .collect(Collectors.groupingBy(
                        r -> r.ExecutionStatus != null ? r.ExecutionStatus : CheckStatus.NOT_STARTED,
                        Collectors.counting()
                    ));

            long executed = byStatus.getOrDefault(CheckStatus.EXECUTED, 0L);
            long skipped = byStatus.getOrDefault(CheckStatus.SKIPPED, 0L);
            long failed = byStatus.getOrDefault(CheckStatus.FAILED, 0L);

            logger.info("Canton {} summary: total={}, executed={}, successful={}, warnings={}, skipped={}, failed={}",
                    canton,
                    result.getResults().size(),
                    executed,
                    result.getSuccessfulCount(),
                    result.getWarningCount(),
                    skipped,
                    failed
            );

            Path outputDirectoryPath = getOutputDirectoryPath(config);
            logger.info("Finalizing Canton {}. Writing results to {} via provider {}", canton, outputDirectoryPath, storageProvider.getClass().getSimpleName());

            ResultManager.write(storageProvider, outputDirectoryPath, result);
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