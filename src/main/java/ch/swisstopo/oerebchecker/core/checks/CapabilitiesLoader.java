package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.config.models.GetCapabilitiesConfig;
import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;
import ch.swisstopo.oerebchecker.models.ResponseFormat;
import ch.swisstopo.oerebchecker.models.ResponseStatusCode;
import ch.swisstopo.oerebchecker.results.CheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CapabilitiesLoader {
    private static final Logger logger = LoggerFactory.getLogger(CapabilitiesLoader.class);

    public static final String CATEGORY = "Capabilities Validation";

    private static final ConcurrentHashMap<URI, CompletableFuture<CapabilitiesLoadResult>> cache = new ConcurrentHashMap<>();

    private CapabilitiesLoader() {}

    public static CapabilitiesLoadResult load(URI basicUri, boolean followOneRedirect) {
        Objects.requireNonNull(basicUri, "basicUri");

        CompletableFuture<CapabilitiesLoadResult> future = cache.computeIfAbsent(basicUri, _ ->
                CompletableFuture.supplyAsync(() -> requestCapabilities(basicUri, followOneRedirect))
        );

        return future.join();
    }

    public static void clear() {
        cache.clear();
    }

    private static CapabilitiesLoadResult requestCapabilities(URI basicUri, boolean followOneRedirect) {
        try {
            GetCapabilitiesConfig cfg = new GetCapabilitiesConfig();
            cfg.FORMAT = ResponseFormat.xml.name();
            cfg.ExpectedStatusCode = ResponseStatusCode.OK;
            cfg.FollowOneRedirect = followOneRedirect;

            GetCapabilities check = new GetCapabilities(basicUri, cfg);
            CheckResult capResult = check.run();

            if (capResult.ExecutionStatus == CheckStatus.SKIPPED) {
                return CapabilitiesLoadResult.failed(
                        ValidatorMessage.error(
                                CATEGORY,
                                "CAPABILITIES_LOAD_SKIPPED",
                                "GetCapabilities was skipped and did not return a valid result.",
                                capResult.Url,
                                capResult.NotExecutedReason
                        )
                );
            }

            if (capResult.ExecutionStatus == CheckStatus.FAILED) {
                return CapabilitiesLoadResult.failed(
                        ValidatorMessage.error(
                                CATEGORY,
                                "CAPABILITIES_LOAD_FAILED",
                                "GetCapabilities execution failed.",
                                capResult.Url,
                                (capResult.ExceptionType != null ? capResult.ExceptionType + ": " : "") + capResult.ExceptionMessage
                        )
                );
            }

            if (capResult.StatusCode != ResponseStatusCode.OK || capResult.XmlIsValid == null || !capResult.XmlIsValid) {
                return CapabilitiesLoadResult.failed(
                        ValidatorMessage.error(
                                CATEGORY,
                                "CAPABILITIES_RESPONSE_INVALID",
                                "GetCapabilities response was not OK or XML validation failed.",
                                capResult.Url,
                                "HTTP " + capResult.StatusCode + ", XmlIsValid=" + capResult.XmlIsValid
                        )
                );
            }

            return new CapabilitiesLoadResult(check.getParsedCapabilities(), List.of());

        } catch (Exception e) {
            logger.error("Failed to load capabilities: {}", e.getMessage(), e);
            return CapabilitiesLoadResult.failed(
                    ValidatorMessage.error(
                            CATEGORY,
                            "CAPABILITIES_LOAD_EXCEPTION",
                            "Failed to load or parse GetCapabilities due to an unexpected exception.",
                            basicUri.toString(),
                            e.getMessage()
                    )
            );
        }
    }
}
