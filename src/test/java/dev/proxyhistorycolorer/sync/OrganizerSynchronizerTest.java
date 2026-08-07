package dev.proxyhistorycolorer.sync;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.organizer.OrganizerItem;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizerSynchronizerTest {
    @Test
    void newestOrganizerItemWinsForDuplicateEndpoint() {
        AtomicReference<String> olderNotes = new AtomicReference<>("Initial notes");
        AtomicReference<String> newerNotes = new AtomicReference<>("Tested successfully");
        AtomicReference<HighlightColor> olderColor = new AtomicReference<>(HighlightColor.RED);
        AtomicReference<HighlightColor> newerColor = new AtomicReference<>(HighlightColor.GREEN);
        OrganizerItem older = itemWithMutableAnnotations(
                3,
                "GET",
                "https://example.com/api/users?page=1",
                olderNotes,
                olderColor
        );
        OrganizerItem newer = itemWithMutableAnnotations(
                8,
                "GET",
                "https://example.com/api/users?page=2",
                newerNotes,
                newerColor
        );

        Map<EndpointKey, EndpointAnnotation> snapshot =
                OrganizerSynchronizer.buildSnapshot(List.of(older, newer));
        OrganizerSynchronizer.buildSnapshot(List.of(older, newer));

        assertEquals(
                new EndpointAnnotation("Tested successfully", HighlightColor.GREEN, 8),
                snapshot.get(EndpointKey.from("GET", "https://example.com/api/users"))
        );
        assertEquals(HighlightColor.PINK, olderColor.get());
        assertEquals(HighlightColor.GREEN, newerColor.get());
        assertEquals(
                "Initial notes\n\n[Proxy History Colorer: superseded by Organizer item #8]",
                olderNotes.get()
        );
        assertEquals("Tested successfully", newerNotes.get());
    }

    @Test
    void uncoloredCommentedItemFallsBackToGray() {
        OrganizerItem item = item(
                1,
                "POST",
                "http://example.com/login",
                "Needs testing",
                null
        );

        EndpointAnnotation annotation = OrganizerSynchronizer.buildSnapshot(List.of(item))
                .get(EndpointKey.from("POST", "http://example.com/login"));

        assertEquals(HighlightColor.GRAY, annotation.color());
        assertEquals("Needs testing", annotation.notes());
    }

    @Test
    void ignoresOrganizerItemsWithoutNotes() {
        OrganizerItem item = item(1, "GET", "https://example.com/", "", HighlightColor.BLUE);

        assertEquals(Map.of(), OrganizerSynchronizer.buildSnapshot(List.of(item)));
    }

    private static OrganizerItem item(
            int id,
            String method,
            String url,
            String notes,
            HighlightColor color
    ) {
        return itemWithMutableAnnotations(
                id,
                method,
                url,
                new AtomicReference<>(notes),
                new AtomicReference<>(color)
        );
    }

    private static OrganizerItem itemWithMutableAnnotations(
            int id,
            String method,
            String url,
            AtomicReference<String> notes,
            AtomicReference<HighlightColor> color
    ) {
        URI uri = URI.create(url);
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort() == -1 ? (secure ? 443 : 80) : uri.getPort();
        HttpService service = proxy(HttpService.class, (invocation, arguments) -> switch (invocation) {
            case "host" -> uri.getHost();
            case "port" -> port;
            case "secure" -> secure;
            default -> null;
        });
        HttpRequest request = proxy(HttpRequest.class, (invocation, arguments) -> switch (invocation) {
            case "method" -> method;
            case "url" -> uri.getRawPath();
            case "httpService" -> service;
            case "pathWithoutQuery" -> uri.getRawPath();
            default -> null;
        });
        Annotations annotations = proxy(Annotations.class, (invocation, arguments) -> switch (invocation) {
            case "hasNotes" -> !notes.get().isBlank();
            case "notes" -> notes.get();
            case "setNotes" -> {
                notes.set((String) arguments[0]);
                yield null;
            }
            case "hasHighlightColor" -> color.get() != null;
            case "highlightColor" -> color.get();
            case "setHighlightColor" -> {
                color.set((HighlightColor) arguments[0]);
                yield null;
            }
            default -> null;
        });
        return proxy(OrganizerItem.class, (invocation, arguments) -> switch (invocation) {
            case "id" -> id;
            case "request" -> request;
            case "annotations" -> annotations;
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodResult result) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, arguments) -> result.value(method.getName(), arguments)
        );
    }

    @FunctionalInterface
    private interface MethodResult {
        Object value(String methodName, Object[] arguments);
    }
}
