package dev.proxyhistorycolorer.model;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record EndpointKey(String method, String scheme, String host, int port, String path) {
    public EndpointKey {
        method = normalizeRequired(method, "method").toUpperCase(Locale.ROOT);
        scheme = normalizeRequired(scheme, "scheme").toLowerCase(Locale.ROOT);
        host = normalizeRequired(host, "host").toLowerCase(Locale.ROOT);
        path = path == null || path.isEmpty() ? "/" : path;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static EndpointKey from(HttpRequest request) {
        Objects.requireNonNull(request, "request");
        URI uri = URI.create(request.url());
        String scheme = requireUriPart(uri.getScheme(), "scheme", request.url());
        String host = requireUriPart(uri.getHost(), "host", request.url());
        int port = uri.getPort() == -1 ? defaultPort(scheme) : uri.getPort();
        return new EndpointKey(request.method(), scheme, host, port, uri.getRawPath());
    }

    public static EndpointKey from(String method, String url) {
        Objects.requireNonNull(url, "url");
        URI uri = URI.create(url);
        String scheme = requireUriPart(uri.getScheme(), "scheme", url);
        String host = requireUriPart(uri.getHost(), "host", url);
        int port = uri.getPort() == -1 ? defaultPort(scheme) : uri.getPort();
        return new EndpointKey(method, scheme, host, port, uri.getRawPath());
    }

    private static int defaultPort(String scheme) {
        return switch (scheme.toLowerCase(Locale.ROOT)) {
            case "http" -> 80;
            case "https" -> 443;
            default -> throw new IllegalArgumentException("URL has no port and uses unsupported scheme: " + scheme);
        };
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String requireUriPart(String value, String name, String url) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL has no " + name + ": " + url);
        }
        return value;
    }
}
