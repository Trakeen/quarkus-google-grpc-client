package io.quarkiverse.google.places;

/**
 * Contract for providing a Google Maps API key at runtime.
 * <p>
 * Implement this interface as a CDI bean in the consuming application to supply
 * the API key (e.g. read from a database or configuration file).
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * public class MyApiKeySupplier implements GoogleApiKeySupplier {
 *     @Override
 *     public String getApiKey() {
 *         return Configuration.streamAll().findFirst().map(Configuration::getGoogleMapsApiKey).orElseThrow();
 *     }
 * }
 * }</pre>
 */
public interface GoogleApiKeySupplier {

    /**
     * Returns a valid Google Maps API key.
     * Called once per gRPC request.
     *
     * @return API key string, never {@code null}.
     */
    String getApiKey();
}
