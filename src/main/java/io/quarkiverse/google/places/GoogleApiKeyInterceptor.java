package io.quarkiverse.google.places;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * gRPC {@link ClientInterceptor} that injects the Google API key into outgoing call metadata.
 * <p>
 * Registered as a {@link GlobalInterceptor} so Quarkus automatically applies it to every
 * gRPC client channel. The field mask ({@code x-goog-fieldmask}) is request-specific and
 * is therefore set directly on each stub in {@code GooglePlacesService}.
 * </p>
 */
@GlobalInterceptor
@ApplicationScoped
public class GoogleApiKeyInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> API_KEY =
            Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    GoogleApiKeySupplier apiKeySupplier;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions,
            final Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(final Listener<RespT> responseListener, final Metadata headers) {
                headers.put(API_KEY, apiKeySupplier.getApiKey());
                super.start(responseListener, headers);
            }
        };
    }
}
