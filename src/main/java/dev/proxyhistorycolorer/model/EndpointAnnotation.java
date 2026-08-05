package dev.proxyhistorycolorer.model;

import burp.api.montoya.core.HighlightColor;

import java.util.Objects;

public record EndpointAnnotation(String notes, HighlightColor color, int organizerItemId) {
    public EndpointAnnotation {
        notes = Objects.requireNonNull(notes, "notes").trim();
        color = Objects.requireNonNull(color, "color");
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("notes must not be blank");
        }
        if (organizerItemId < 0) {
            throw new IllegalArgumentException("organizerItemId must not be negative");
        }
    }
}
