package io.quarkiverse.google.places.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Place details returned by {@code GetPlace} or {@code SearchNearby}.
 */
@Builder
@Getter
public class PlaceAddress {

    /** Google Place ID. */
    private String id;

    /** Short display name. */
    private String name;

    /** Full formatted address. */
    private String formattedAddress;

    /** Longitude. */
    private Double lng;

    /** Latitude. */
    private Double lat;

    /** Google Maps URI. */
    private String googleMapsUri;

    /** International phone number, e.g. {@code +33 1 23 45 67 89}. */
    private String phoneNumber;

    /** Rating out of 5, or {@code null} if not rated. */
    private Double rating;

    /** Place type strings, e.g. {@code ["gas_station", "point_of_interest"]}. */
    private List<String> types;
}
