package ch.swisstopo.oerebchecker.core.checks;

import java.util.List;

public record CapabilitiesData(List<String> topicCodes, List<String> languages) {

    public static CapabilitiesData empty() {
        return new CapabilitiesData(List.of(), List.of());
    }
}
