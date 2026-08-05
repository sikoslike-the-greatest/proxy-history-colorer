package dev.proxyhistorycolorer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EndpointKeyTest {
    @Test
    void ignoresQueryAndNormalizesMethodHostAndDefaultPort() {
        EndpointKey first = EndpointKey.from("get", "https://EXAMPLE.com/api/users?id=1");
        EndpointKey second = EndpointKey.from("GET", "https://example.com:443/api/users?id=2");

        assertEquals(first, second);
        assertEquals(new EndpointKey("GET", "https", "example.com", 443, "/api/users"), first);
    }

    @Test
    void keepsHttpMethodsSeparate() {
        EndpointKey get = EndpointKey.from("GET", "https://example.com/api/users");
        EndpointKey post = EndpointKey.from("POST", "https://example.com/api/users");

        assertNotEquals(get, post);
    }

    @Test
    void keepsExplicitNonDefaultPortsSeparate() {
        EndpointKey standard = EndpointKey.from("GET", "https://example.com/path");
        EndpointKey alternate = EndpointKey.from("GET", "https://example.com:8443/path");

        assertNotEquals(standard, alternate);
    }

    @Test
    void normalizesEmptyPathToSlash() {
        assertEquals(
                EndpointKey.from("GET", "http://example.com/"),
                EndpointKey.from("GET", "http://example.com")
        );
    }
}
