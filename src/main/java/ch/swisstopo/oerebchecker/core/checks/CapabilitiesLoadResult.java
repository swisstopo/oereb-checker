package ch.swisstopo.oerebchecker.core.checks;

import ch.swisstopo.oerebchecker.core.validation.ValidatorMessage;

import java.util.List;

/**
 * Outcome of a GetCapabilities lookup: the parsed data plus the diagnostics produced while
 * loading it. The messages travel with the data because the lookup is shared per endpoint,
 * so every check that uses it can report the load failure on its own result.
 */
public record CapabilitiesLoadResult(CapabilitiesData data, List<ValidatorMessage> messages) {

    public static CapabilitiesLoadResult empty() {
        return new CapabilitiesLoadResult(CapabilitiesData.empty(), List.of());
    }

    public static CapabilitiesLoadResult failed(ValidatorMessage message) {
        return new CapabilitiesLoadResult(CapabilitiesData.empty(), List.of(message));
    }
}
