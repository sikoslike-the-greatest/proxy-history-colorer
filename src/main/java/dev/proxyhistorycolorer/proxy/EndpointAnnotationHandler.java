package dev.proxyhistorycolorer.proxy;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import dev.proxyhistorycolorer.model.EndpointAnnotation;
import dev.proxyhistorycolorer.model.EndpointKey;
import dev.proxyhistorycolorer.store.EndpointRegistry;

import java.util.Objects;
import java.util.Optional;

public final class EndpointAnnotationHandler implements ProxyRequestHandler {
    private final EndpointRegistry registry;

    public EndpointAnnotationHandler(EndpointRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        Optional<EndpointAnnotation> match = find(interceptedRequest);
        if (match.isEmpty()) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }

        EndpointAnnotation annotation = match.get();
        Annotations annotations = Annotations.annotations(annotation.notes(), annotation.color());
        return ProxyRequestReceivedAction.continueWith(interceptedRequest, annotations);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    private Optional<EndpointAnnotation> find(InterceptedRequest request) {
        try {
            return registry.find(EndpointKey.from(request));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
