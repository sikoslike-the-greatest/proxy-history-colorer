package dev.proxyhistorycolorer.store;

import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class EndpointRegistry {
    private final AtomicReference<Map<EndpointKey, EndpointAnnotation>> entries =
            new AtomicReference<>(Map.of());

    public Optional<EndpointAnnotation> find(EndpointKey key) {
        return Optional.ofNullable(entries.get().get(Objects.requireNonNull(key, "key")));
    }

    public Map<EndpointKey, EndpointAnnotation> snapshot() {
        return entries.get();
    }

    public boolean replace(Map<EndpointKey, EndpointAnnotation> replacement) {
        Map<EndpointKey, EndpointAnnotation> immutable = Map.copyOf(replacement);
        return !entries.getAndSet(immutable).equals(immutable);
    }
}
