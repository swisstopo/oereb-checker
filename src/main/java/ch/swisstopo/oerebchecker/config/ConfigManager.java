package ch.swisstopo.oerebchecker.config;

import ch.swisstopo.oerebchecker.CliParser;
import ch.swisstopo.oerebchecker.config.models.*;
import ch.swisstopo.oerebchecker.storage.S3StorageProvider;
import ch.swisstopo.oerebchecker.models.Canton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private static final String s3ConfigEnvKey = "S3Config";

    private static Path configFilePath;
    private static final Map<Canton, CantonConfig> cantonConfigs = new Hashtable<>();

    public static Map<Canton, CantonConfig> getCantonConfigs() {
        return cantonConfigs;
    }

    public static void loadConfigs(S3StorageProvider storageProvider) {

        String configFilePathString = System.getenv(s3ConfigEnvKey);

        if (configFilePathString == null || configFilePathString.isBlank()) {
            logger.error("No value for environment variable: '{}'", s3ConfigEnvKey);
        } else {
            configFilePath = Paths.get(configFilePathString);
            byte[] cantonConfigData = storageProvider.readObject(configFilePath);
            CantonConfig cantonConfig = getCantonConfig(configFilePath, cantonConfigData);
            if (cantonConfig != null) {
                Canton canton = getCantonFromConfigOrPath(cantonConfig, configFilePath);
                cantonConfigs.put(canton, cantonConfig);
            }
        }
    }

    public static boolean loadConfigs(CliParser cli) {

        String configFilePathString = cli.getConfigFilePath();

        if (configFilePathString != null) {
            try {
                configFilePath = Paths.get(configFilePathString).toAbsolutePath();

                if (!Files.exists(configFilePath) && !Files.isDirectory(configFilePath)) {
                    logger.error("No file or directory exists with path: '{}'", configFilePathString);
                    return false;

                } else if (Files.isDirectory(configFilePath)) {

                    try (Stream<Path> subDirectoryStream = Files.walk(configFilePath, 1).filter(Files::isDirectory)) {
                        List<Path> subDirectoryPaths = subDirectoryStream.collect(Collectors.toList());
                        subDirectoryPaths.removeFirst();

                        for (Path subDirectoryPath : subDirectoryPaths) {
                            try (Stream<Path> fileStream = Files.walk(subDirectoryPath, 1).filter(Files::isRegularFile)) {
                                List<Path> filePaths = fileStream.toList();
                                if (!filePaths.isEmpty()) {
                                    byte[] cantonConfigData = Files.readAllBytes(filePaths.getFirst());
                                    CantonConfig cantonConfig = getCantonConfig(filePaths.getFirst(), cantonConfigData);
                                    if (cantonConfig != null) {
                                        Canton canton = getCantonFromConfigOrPath(cantonConfig, filePaths.getFirst());
                                        cantonConfigs.put(canton, cantonConfig);
                                    }
                                }
                            }
                        }
                        return true;

                    } catch (Exception e) {
                        logger.error("Error reading config/-s: '{}'", configFilePathString, e);
                    }

                } else {
                    byte[] cantonConfigData = Files.readAllBytes(configFilePath);
                    CantonConfig cantonConfig = getCantonConfig(configFilePath, cantonConfigData);
                    if (cantonConfig != null) {
                        Canton canton = getCantonFromConfigOrPath(cantonConfig, configFilePath);
                        cantonConfigs.put(canton, cantonConfig);
                    }
                    return true;
                }
            } catch (IOException e) {
                logger.error("Can not read config file: {}", configFilePathString, e);
            }
        }
        // Handle manual configuration via console parameters
        if (cli.getBaseUrl() != null && cli.getCanton() != null) {
            try {
                Canton canton = Canton.valueOf(cli.getCanton().toUpperCase());
                CantonConfig manualConfig = new CantonConfig();
                manualConfig.BasicUrl = cli.getBaseUrl();
                manualConfig.OutputDirectoryPath = cli.getOutputDirectoryPath();

                String type = cli.getType();
                String format = cli.getFormat() != null ? cli.getFormat() : "xml";
                Integer expectedStatus = cli.getExpectedStatusCode();

                if ("GetVersions".equalsIgnoreCase(type)) {
                    var c = new GetVersionsConfig();
                    c.ExpectedStatusCode = expectedStatus;
                    c.FORMAT = format;

                    if (!c.isValid()) {
                        throw new IllegalArgumentException("Invalid GetVersions params");
                    }
                    manualConfig.GetVersions.add(c);

                } else if ("GetCapabilities".equalsIgnoreCase(type)) {
                    var c = new GetCapabilitiesConfig();
                    c.ExpectedStatusCode = expectedStatus;
                    c.FORMAT = format;

                    if (!c.isValid()) {
                        throw new IllegalArgumentException("Invalid GetCapabilities params");
                    }
                    manualConfig.GetCapabilities.add(c);

                } else if ("GetEGRID".equalsIgnoreCase(type)) {
                    var c = new GetEGRIDConfig();
                    c.ExpectedStatusCode = expectedStatus;
                    c.FORMAT = format;
                    c.EN = cli.getParam("EN");
                    c.IDENTDN = cli.getParam("IDENTDN");
                    c.NUMBER = cli.getParam("NUMBER");
                    c.GNSS = cli.getParam("GNSS");
                    if (cli.getParam("POSTALCODE") != null) {
                        c.POSTALCODE = Integer.parseInt(cli.getParam("POSTALCODE"));
                    }

                    if (!c.isValid()) {
                        throw new IllegalArgumentException("Invalid GetEGRID parameter combination.");
                    }
                    manualConfig.GetEGRID.add(c);

                } else if ("GetExtractById".equalsIgnoreCase(type)) {
                    var c = new GetExtractByIdConfig();
                    c.ExpectedStatusCode = expectedStatus;
                    c.FORMAT = format;
                    c.EGRID = cli.getParam("EGRID");
                    c.IDENTDN = cli.getParam("IDENTDN");
                    c.NUMBER = cli.getParam("NUMBER");
                    c.LANG = cli.getParam("LANG");
                    c.TOPICS = cli.getParam("TOPICS");
                    if (cli.getParam("GEOMETRY") != null) {
                        c.GEOMETRY = Boolean.parseBoolean(cli.getParam("GEOMETRY"));
                    }
                    if (cli.getParam("WITHIMAGES") != null) {
                        c.WITHIMAGES = Boolean.parseBoolean(cli.getParam("WITHIMAGES"));
                    }

                    if (!c.isValid()) {
                        throw new IllegalArgumentException("Invalid GetExtractById parameter combination.");
                    }
                    manualConfig.GetExtractById.add(c);
                }

                cantonConfigs.put(canton, manualConfig);
                return true;

            } catch (Exception e) {
                logger.error("Failed to build manual config from parameters: {}", e.getMessage());
                throw new RuntimeException("Application stop: Manual configuration is invalid.", e);
            }
        }
        return false;
    }

    private static Canton getCantonFromConfigOrPath(CantonConfig cantonConfig, Path configFilePath) {

        if (!StringUtils.isBlank(cantonConfig.Canton)) {
            try {
                return Canton.valueOf(cantonConfig.Canton.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid canton name '{}' in config file from path: '{}'", cantonConfig.Canton, configFilePath);
            }
        }

        try {
            String directoryName = configFilePath.getParent().getFileName().toString();
            return Canton.valueOf(directoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Can not determine canton from path: {}", configFilePath);
            return Canton.unknown;
        }
    }

    private static CantonConfig getCantonConfig(Path configFilePath, byte[] cantonConfigData) {

        if (cantonConfigData != null) {
            String configContent = new String(cantonConfigData, StandardCharsets.UTF_8);
            String fileName = configFilePath.getFileName().toString();
            if (fileName.endsWith(".json")) {
                try {
                    Gson gson = new Gson();
                    return gson.fromJson(configContent, CantonConfig.class);
                } catch (JsonSyntaxException e) {
                    throw new RuntimeException(e);
                }
            } else if (fileName.endsWith(".xml")) {
                CantonConfig cantonConfig;
                try {
                    StringReader sr = new StringReader(configContent);
                    JAXBContext jaxbContext = JAXBContext.newInstance(CantonConfig.class);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    cantonConfig = (CantonConfig) unmarshaller.unmarshal(sr);
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
                return cantonConfig;
            }
        }
        logger.error("Canton config file content not read or found in config file: '{}'", configFilePath);
        return null;
    }
}
