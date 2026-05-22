package io.quarkiverse.google.places;

import com.google.maps.places.v1.*;
import com.google.type.LatLng;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.quarkus.grpc.GrpcClient;
import io.quarkiverse.google.places.model.PlaceAddress;
import io.quarkiverse.google.places.model.PlaceSuggestion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;



/**
 * Quarkus CDI service wrapping the Google Places API (New) gRPC interface.
 *
 * <p>The consuming application must:</p>
 * <ol>
 *   <li>Provide a {@link GoogleApiKeySupplier} CDI bean.</li>
 *   <li>Configure the gRPC client in {@code application.properties}:
 *   <pre>
 *   quarkus.grpc.clients.google-places.host=places.googleapis.com
 *   quarkus.grpc.clients.google-places.port=443
 *   quarkus.grpc.clients.google-places.ssl=true
 *   </pre>
 *   </li>
 * </ol>
 */
@ApplicationScoped
public class GooglePlacesService {

    /** Field mask for GetPlace — only the fields we actually use. */
    private static final String PLACE_FIELDS =
            "id,displayName,formattedAddress,location,internationalPhoneNumber,rating,googleMapsUri";

    /** Field mask for SearchNearby / SearchText — same fields but prefixed with "places.". */
    private static final String NEARBY_FIELDS =
            "places.id,places.displayName,places.formattedAddress,places.location," +
            "places.internationalPhoneNumber,places.rating,places.googleMapsUri,places.types";

    @GrpcClient("google-places")
    Channel channel;

    @Inject
    GoogleApiKeySupplier apiKeySupplier;

    // -------------------------------------------------------------------------
    // AutocompletePlaces
    // -------------------------------------------------------------------------

    /**
     * Returns autocomplete suggestions for a partial address or POI name.
     *
     * @param input      User input text.
     * @param centerLat  Latitude of the search bias centre.
     * @param centerLng  Longitude of the search bias centre.
     * @param radiusM    Search bias radius in metres.
     * @param language   IETF language tag, e.g. {@code "fr"}.
     * @return List of {@link PlaceSuggestion}.
     */
    public List<PlaceSuggestion> autocompletePlaces(
            final String input,
            final double centerLat,
            final double centerLng,
            final int radiusM,
            final String language) {

        final PlacesGrpc.PlacesBlockingStub stub = createStub(null);

        final AutocompletePlacesRequest request = AutocompletePlacesRequest.newBuilder()
                .setInput(input)
                .setLanguageCode(language)
                .setLocationBias(AutocompletePlacesRequest.LocationBias.newBuilder()
                        .setCircle(Circle.newBuilder()
                                .setCenter(LatLng.newBuilder()
                                        .setLatitude(centerLat)
                                        .setLongitude(centerLng)
                                        .build())
                                .setRadius(radiusM)
                                .build())
                        .build())
                .build();

        final AutocompletePlacesResponse response = stub.autocompletePlaces(request);

        return response.getSuggestionsList().stream()
                .filter(AutocompletePlacesResponse.Suggestion::hasPlacePrediction)
                .map(AutocompletePlacesResponse.Suggestion::getPlacePrediction)
                .map(p -> PlaceSuggestion.builder()
                        .placeId(p.getPlaceId())
                        .mainText(p.hasStructuredFormat()
                                ? p.getStructuredFormat().getMainText().getText()
                                : p.getText().getText())
                        .fullText(p.getText().getText())
                        .distanceMeters(p.getDistanceMeters() > 0 ? p.getDistanceMeters() : null)
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // GetPlace
    // -------------------------------------------------------------------------

    /**
     * Returns full details for a place identified by its Google Place ID.
     *
     * @param placeId  Google Place ID (e.g. {@code ChIJN1t_tDeuEmsRUsoyG83frY4}).
     * @param language IETF language tag.
     * @return {@link PlaceAddress} with coordinates, phone, rating and URL.
     */
    public PlaceAddress getPlace(final String placeId, final String language) {

        final PlacesGrpc.PlacesBlockingStub stub = createStub(PLACE_FIELDS);

        final GetPlaceRequest request = GetPlaceRequest.newBuilder()
                .setName("places/" + placeId)
                .setLanguageCode(language)
                .build();

        final Place place = stub.getPlace(request);

        return toPlaceAddress(place);
    }

    // -------------------------------------------------------------------------
    // SearchNearby
    // -------------------------------------------------------------------------

    /**
     * Searches for POIs near a location filtered by place type.
     *
     * @param lat          Centre latitude.
     * @param lng          Centre longitude.
     * @param radiusM      Search radius in metres.
     * @param types        Included place type strings (e.g. {@code "gas_station"}).
     * @param language     IETF language tag.
     * @param maxResults   Maximum number of results (1–20).
     * @return List of nearby {@link PlaceAddress}.
     */
    public List<PlaceAddress> searchNearby(
            final double lat,
            final double lng,
            final int radiusM,
            final List<String> types,
            final String language,
            final int maxResults) {

        final PlacesGrpc.PlacesBlockingStub stub = createStub(NEARBY_FIELDS);

        final SearchNearbyRequest request = SearchNearbyRequest.newBuilder()
                .setLanguageCode(language)
                .addAllIncludedTypes(types)
                .setMaxResultCount(Math.min(maxResults, 20))
                .setLocationRestriction(SearchNearbyRequest.LocationRestriction.newBuilder()
                        .setCircle(Circle.newBuilder()
                                .setCenter(LatLng.newBuilder()
                                        .setLatitude(lat)
                                        .setLongitude(lng)
                                        .build())
                                .setRadius(radiusM)
                                .build())
                        .build())
                .build();

        final SearchNearbyResponse response = stub.searchNearby(request);

        return response.getPlacesList().stream()
                .map(this::toPlaceAddress)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // SearchText
    // -------------------------------------------------------------------------

    /**
     * Searches for places matching a free-text query, biased around a location.
     *
     * @param query      Free-text query (e.g. POI name or address fragment).
     * @param lat        Bias centre latitude.
     * @param lng        Bias centre longitude.
     * @param radiusM    Bias radius in metres.
     * @param language   IETF language tag.
     * @param maxResults Maximum number of results (1–20).
     * @return List of matching {@link PlaceAddress}.
     */
    public List<PlaceAddress> searchText(
            final String query,
            final double lat,
            final double lng,
            final int radiusM,
            final String language,
            final int maxResults) {

        final PlacesGrpc.PlacesBlockingStub stub = createStub(NEARBY_FIELDS);

        final SearchTextRequest request = SearchTextRequest.newBuilder()
                .setTextQuery(query)
                .setLanguageCode(language)
                .setMaxResultCount(Math.min(maxResults, 20))
                .setLocationBias(SearchTextRequest.LocationBias.newBuilder()
                        .setCircle(Circle.newBuilder()
                                .setCenter(LatLng.newBuilder()
                                        .setLatitude(lat)
                                        .setLongitude(lng)
                                        .build())
                                .setRadius(radiusM)
                                .build())
                        .build())
                .build();

        final SearchTextResponse response = stub.searchText(request);

        return response.getPlacesList().stream()
                .map(this::toPlaceAddress)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PlacesGrpc.PlacesBlockingStub createStub(final String fieldMask) {
        return PlacesGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, new GoogleApiKeyInterceptor(apiKeySupplier, fieldMask)));
    }

    private PlaceAddress toPlaceAddress(final Place place) {
        return PlaceAddress.builder()
                .id(place.getId())
                .name(place.hasDisplayName() ? place.getDisplayName().getText() : null)
                .formattedAddress(place.getFormattedAddress())
                .lat(place.hasLocation() ? place.getLocation().getLatitude() : null)
                .lng(place.hasLocation() ? place.getLocation().getLongitude() : null)
                .googleMapsUri(place.getGoogleMapsUri())
                .phoneNumber(place.getInternationalPhoneNumber())
                .rating(place.getRating() > 0 ? place.getRating() : null)
                .types(place.getTypesList())
                .build();
    }
}
