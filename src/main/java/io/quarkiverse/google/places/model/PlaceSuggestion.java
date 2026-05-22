package io.quarkiverse.google.places.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Autocomplete suggestion returned by {@code AutocompletePlaces}.
 */
@Builder
@Getter
public class PlaceSuggestion {

    /** Google Place ID. */
    private String placeId;

    /** Short display name (main text). */
    private String mainText;

    /** Full formatted text. */
    private String fullText;

    /** Distance in metres from the origin point, or {@code null} if unavailable. */
    private Integer distanceMeters;
}
