package dev.proxyhistorycolorer.store;

import burp.api.montoya.core.HighlightColor;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryCodecTest {
    private final RegistryCodec codec = new RegistryCodec();

    @Test
    void roundTripsUnicodeNotesAndEndpointMetadata() {
        EndpointKey key = EndpointKey.from("POST", "https://example.com:8443/api/%D1%82%D0%B5%D1%81%D1%82?q=1");
        Map<EndpointKey, EndpointAnnotation> original = Map.of(
                key,
                new EndpointAnnotation("Проверить повторно\nIDOR", HighlightColor.RED, 42)
        );

        assertEquals(original, codec.decode(codec.encode(original)));
    }

    @Test
    void emptyOrMissingStorageLoadsAsEmptyRegistry() {
        assertEquals(Map.of(), codec.decode(null));
        assertEquals(Map.of(), codec.decode(""));
    }
}
