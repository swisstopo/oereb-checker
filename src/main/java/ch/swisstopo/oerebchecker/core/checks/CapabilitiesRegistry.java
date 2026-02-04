package ch.swisstopo.oerebchecker.core.checks;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CapabilitiesRegistry {

    private static final ConcurrentHashMap<URI, CompletableFuture<CapabilitiesData>> cache = new ConcurrentHashMap<>();

    private CapabilitiesRegistry() {}

    public static CapabilitiesData getOrLoad(URI basicUri, Supplier<CapabilitiesData> loader) {
        Objects.requireNonNull(basicUri, "basicUri");
        Objects.requireNonNull(loader, "loader");

        CompletableFuture<CapabilitiesData> future = cache.computeIfAbsent(basicUri, _uri ->
                CompletableFuture.supplyAsync(() -> {
                    CapabilitiesData data = loader.get();
                    return data != null ? data : CapabilitiesData.empty();
                })
        );

        return future.join();
    }

    public static void clear() {
        cache.clear();
    }
}