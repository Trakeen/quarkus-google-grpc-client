package io.quarkiverse.google.places;

import io.grpc.*;

/**
 * gRPC {@link ClientInterceptor} that injects the Google API key and an optional
 * field mask into outgoing call metadata.
 */
public class GoogleApiKeyInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> API_KEY =
            Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> FIELD_MASK =
            Metadata.Key.of("x-goog-fieldmask", Metadata.ASCII_STRING_MARSHALLER);

    private final GoogleApiKeySupplier apiKeySupplier;
    private final String fieldMask;

    /**
     * Creates an interceptor that only injects the API key.
     */
    public GoogleApiKeyInterceptor(final GoogleApiKeySupplier apiKeySupplier) {
        this(apiKeySupplier, null);
    }

    /**
     * Creates an interceptor that injects both the API key and a field mask.
     *
     * @param fieldMask Comma-separated list of fields to return, e.g.
     *                  {@code "id,displayName,location"}.
     */
    public GoogleApiKeyInterceptor(final GoogleApiKeySupplier apiKeySupplier, final String fieldMask) {
        this.apiKeySupplier = apiKeySupplier;
        this.fieldMask = fieldMask;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions,
            final Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(final Listener<RespT> responseListener, final Metadata headers) {
                headers.put(API_KEY, apiKeySupplier.getApiKey());
                if (fieldMask != null && !fieldMask.isBlank()) {
                    headers.put(FIELD_MASK, fieldMask);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
