package dev.proxyhistorycolorer.store;

import burp.api.montoya.persistence.PersistedObject;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;

import java.util.Map;
import java.util.Objects;

public final class ProjectStore {
    static final String STORAGE_KEY = "proxy-history-colorer.endpoint-registry";

    private final PersistedObject persistedObject;
    private final RegistryCodec codec;

    public ProjectStore(PersistedObject persistedObject, RegistryCodec codec) {
        this.persistedObject = Objects.requireNonNull(persistedObject, "persistedObject");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public Map<EndpointKey, EndpointAnnotation> load() {
        return codec.decode(persistedObject.getString(STORAGE_KEY));
    }

    public void save(Map<EndpointKey, EndpointAnnotation> entries) {
        persistedObject.setString(STORAGE_KEY, codec.encode(entries));
    }
}
