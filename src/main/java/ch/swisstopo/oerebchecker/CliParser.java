package ch.swisstopo.oerebchecker;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliParser {
    private static final Logger logger = LoggerFactory.getLogger(CliParser.class);

    private final Map<String, String> options = new HashMap<>();

    public CliParser(String[] args) {
        parse(args);
    }

    private void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && i + 1 < args.length) {
                options.put(args[i].toLowerCase(), args[i + 1]);
                i++;
            }
        }
    }

    public String getKey() {
        return getParam("Key");
    }

    public boolean isHelpRequested() {
        return options.containsKey("-help") || options.containsKey("-info");
    }

    public String getConfigFilePath() {
        return getParam("cFP");
    }

    public String getOutputDirectoryPath() {
        return getParam("oDP");
    }

    public String getCanton() {
        return getParam("canton");
    }

    public String getBaseUrl() {
        return getParam("baseurl");
    }

    public String getType() {
        return getParam("type");
    }

    public String getFormat() {
        return getParam("format");
    }

    public boolean getProvoke500() {
        String val = getParam("provoke500");
        return Boolean.parseBoolean(val);
    }

    public Integer getExpectedStatusCode() {
        String val = getParam("eSC");
        return val != null ? Integer.parseInt(val) : 200;
    }

    public String getParam(String key, String defaultValue) {
        return options.getOrDefault("-" + key.toLowerCase(), defaultValue);
    }

    public String getParam(String key) {
        return getParam(key, null);
    }

    public void printHelp() {
        logger.info("OeREB Checker - Usage Instructions:");
        logger.info("  -cFP [path]   : Custom configuration file path (XML/JSON)");
        logger.info("  -oDP [path]   : Custom output directory path");
        logger.info("  -canton [XX]  : Canton code (e.g., ZH, BE, AG)");
        logger.info("  -baseurl [url]: Base URL of the OeREB service");
        logger.info("  -type [type]  : Check type (GetVersions, GetCapabilities, GetEGRID, GetExtractById)");
        logger.info("  -format [fmt] : Response format (xml, json, pdf)");
        logger.info("  -eSC [code]   : Expected HTTP status code (e.g., 200, 303)");
        logger.info("");
        logger.info("Optional General Parameters:");
        logger.info("  -provoke500 [bool]: Intentionally trigger a server error by requesting an invalid format (true/false)");
        logger.info("");
        logger.info("Specific Check Parameters:");
        logger.info("  [GetEGRID] - Requires one of the following combinations:");
        logger.info("    Combination 1: -EN [coords]");
        logger.info("    Combination 2: -IDENTDN [id] AND -NUMBER [num]");
        logger.info("    Combination 3: -POSTALCODE [zip] AND -LOCALISATION [name] AND -NUMBER [num]");
        logger.info("    Combination 4: -GNSS [coords]");
        logger.info("");
        logger.info("  [GetExtractById] - Requires one of the following combinations:");
        logger.info("    Combination 1: -EGRID [id]");
        logger.info("    Combination 2: -IDENTDN [id] AND -NUMBER [num]");
        logger.info("");
        logger.info("  Optional Extract Parameters:");
        logger.info("    -LANG [lang]      : Language (de, fr, it, rm, en)");
        logger.info("    -TOPICS [topics]  : Comma-separated list of topics");
        logger.info("    -GEOMETRY [bool]  : Include geometry (true/false)");
        logger.info("    -WITHIMAGES [bool]: Include images (true/false)");
        logger.info("");
        logger.info("  -help         : Display this help information");
        logger.info("");
        logger.info("Examples:");
        logger.info("  java -jar oereb-checker.jar -cFP ./config/be/config.json");
        logger.info("  java -jar oereb-checker.jar -canton BE -baseurl https://www.oereb2.apps.be.ch -type GetEGRID -EN 2606296.340,1215309.120 -format json -eSC 200 -oDP ./my-result");
        logger.info("  java -jar oereb-checker.jar -canton BE -baseurl https://www.oereb2.apps.be.ch -type GetExtractById -EGRID CH123456789 -format xml -GEOMETRY true -eSC 200 -provoke500 true -oDP ./my-result");
    }
}