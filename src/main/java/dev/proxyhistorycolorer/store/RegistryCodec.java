package dev.proxyhistorycolorer.store;

import burp.api.montoya.core.HighlightColor;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class RegistryCodec {
    private static final String VERSION = "v1";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public String encode(Map<EndpointKey, EndpointAnnotation> entries) {
        StringBuilder result = new StringBuilder(VERSION);
        entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((left, right) -> canonical(left).compareTo(canonical(right))))
                .forEach(entry -> append(result, entry.getKey(), entry.getValue()));
        return result.toString();
    }

    public Map<EndpointKey, EndpointAnnotation> decode(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return Map.of();
        }

        String[] lines = serialized.split("\\R");
        if (lines.length == 0 || !VERSION.equals(lines[0])) {
            throw new IllegalArgumentException("Unsupported endpoint registry format");
        }

        Map<EndpointKey, EndpointAnnotation> result = new HashMap<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] fields = lines[index].split("\\t", -1);
            if (fields.length != 8) {
                throw new IllegalArgumentException("Invalid endpoint registry row " + index);
            }
            EndpointKey key = new EndpointKey(
                    decodeField(fields[0]),
                    decodeField(fields[1]),
                    decodeField(fields[2]),
                    Integer.parseInt(fields[3]),
                    decodeField(fields[4])
            );
            EndpointAnnotation annotation = new EndpointAnnotation(
                    decodeField(fields[5]),
                    HighlightColor.valueOf(fields[6]),
                    Integer.parseInt(fields[7])
            );
            result.merge(key, annotation, RegistryCodec::newest);
        }
        return Map.copyOf(result);
    }

    private static void append(
            StringBuilder target,
            EndpointKey key,
            EndpointAnnotation annotation
    ) {
        target.append('\n')
                .append(encodeField(key.method())).append('\t')
                .append(encodeField(key.scheme())).append('\t')
                .append(encodeField(key.host())).append('\t')
                .append(key.port()).append('\t')
                .append(encodeField(key.path())).append('\t')
                .append(encodeField(annotation.notes())).append('\t')
                .append(annotation.color().name()).append('\t')
                .append(annotation.organizerItemId());
    }

    private static EndpointAnnotation newest(
            EndpointAnnotation left,
            EndpointAnnotation right
    ) {
        return left.organizerItemId() >= right.organizerItemId() ? left : right;
    }

    private static String canonical(EndpointKey key) {
        return key.method() + '\0' + key.scheme() + '\0' + key.host() + '\0' + key.port() + '\0' + key.path();
    }

    private static String encodeField(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
}
