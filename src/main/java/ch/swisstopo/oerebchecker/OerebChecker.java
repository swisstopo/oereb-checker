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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OerebChecker {
    private static final Logger logger = LoggerFactory.getLogger(OerebChecker.class);

    private static IStorageProvider storageProvider = new LocalStorageProvider();

    public static void main(String[] args) {
        try {
            execute(args);
            System.exit(0);
        } catch (Exception ex) {
            logger.error("Critical failure: {}", ex.getMessage(), ex);
            System.exit(1);
        }
    }

    public static void execute(String[] args) throws Exception {
        CliParser cli = new CliParser(args);
        logger.trace("Application started with arguments: {}", String.join(" ", args));

        if (cli.isHelpRequested()) {
            cli.printHelp();
            System.exit(0);
        }

        // 1. Initialize Storage
        S3StorageProvider.createFromEnv("S3ResultBucket").ifPresent(provider -> {
            storageProvider = provider;
            logger.info("S3 Storage initialized.");
        });

        // 2. Load Config
        boolean localConfigLoaded = ConfigManager.loadConfigs(cli);

        if (!localConfigLoaded && storageProvider instanceof S3StorageProvider) {
            logger.info("Local config not found, attempting to load from S3 config bucket...");
            S3StorageProvider.createFromEnv("S3ConfigBucket")
                    .ifPresent(ConfigManager::loadConfigs);
        }

        if (ConfigManager.getCantonConfigs().isEmpty()) {
            logger.error("Application configuration is empty or could not be parsed. Please check your config source.");
            System.exit(1);
        }

        // 3. Load Metadata
        MetadataManager.loadMetadata();

        // 4. Execution Loop
        for (Canton canton : ConfigManager.getCantonConfigs().keySet()) {
            processCanton(canton);
        }

        logger.info("All checks completed successfully.");
    }

    private static void processCanton(Canton canton) {

        var config = ConfigManager.getCantonConfigs().get(canton);
        logger.trace("Processing Canton: {} with base URL: {}", canton, config.BasicUrl);

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

            CantonResult result = new CantonResult(canton);

            futures.forEach(f -> {
                try {
                    result.addCheckResult(f.get());
                } catch (Exception e) {
                    logger.error("Check failed", e);
                }
            });

            Path outputDirectoryPath = getOutputDirectoryPath(config);
            logger.info("Finalizing Canton {}. Writing results to {} via provider {}", canton, outputDirectoryPath, storageProvider.getClass().getSimpleName());

            ResultManager.write(storageProvider, outputDirectoryPath, result);
        }
    }

    private static Path getOutputDirectoryPath(CantonConfig config) {

        String envPath = System.getenv("S3ResultOutputPath");
        if (envPath != null) {
            logger.trace("Found & use S3 Result Output Path: {}", envPath);
            return Paths.get(envPath);
        }
        envPath = System.getenv("OUTPUT_PATH");
        if (envPath != null) {
            logger.trace("Found & use OUTPUT_PATH: {}", envPath);
            return Paths.get(envPath).toAbsolutePath();
        }
        if (config.OutputDirectoryPath != null) {
            logger.trace("Found & use OutputDirectoryPath: {}", config.OutputDirectoryPath);
            return Paths.get(config.OutputDirectoryPath).toAbsolutePath();
        }

        return Path.of("").toAbsolutePath().resolve("output");
    }
}