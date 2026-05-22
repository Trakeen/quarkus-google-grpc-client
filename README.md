# quarkus-google-grpc-client

Quarkus library wrapping the **Google Places API (New)** gRPC interface.  
Designed for applications that have enabled **Places API (New)** in Google Cloud Console (not the legacy Places API).

---

## Features

| Method | API call |
|---|---|
| `autocompletePlaces` | `AutocompletePlaces` — address/POI autocomplete suggestions |
| `getPlace` | `GetPlace` — full details for a known Place ID |
| `searchNearby` | `SearchNearby` — POIs near a location filtered by type |

---

## Requirements

- Java 17+
- Quarkus 3.35.x
- A Google Cloud project with **Places API (New)** enabled
- A server-side API key (no HTTP referrer restriction)

---

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkiverse.google</groupId>
    <artifactId>quarkus-google-grpc-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

> The library brings `quarkus-grpc` transitively. If Quarkus does not pick it up automatically, declare it explicitly:
>
> ```xml
> <dependency>
>     <groupId>io.quarkus</groupId>
>     <artifactId>quarkus-grpc</artifactId>
> </dependency>
> ```

---

## Configuration

Add the following to `application.properties`:

```properties
quarkus.grpc.clients.google-places.host=places.googleapis.com
quarkus.grpc.clients.google-places.port=443
quarkus.grpc.clients.google-places.ssl=true
```

---

## Usage

### 1. Implement `GoogleApiKeySupplier`

Provide a CDI bean that returns your Google API key at runtime (e.g. read from a database or config):

```java
@ApplicationScoped
public class MyApiKeySupplier implements GoogleApiKeySupplier {

    @Override
    public String getApiKey() {
        return Configuration.streamAll()
                .findFirst()
                .map(Configuration::getGoogleMapsApiKey)
                .orElseThrow();
    }
}
```

### 2. Inject `GooglePlacesService`

```java
@ApplicationScoped
public class MyService {

    @Inject
    GooglePlacesService googlePlacesService;

    // Autocomplete — returns suggestions without coordinates
    public List<PlaceSuggestion> search(String input) {
        return googlePlacesService.autocompletePlaces(
                input,
                48.8566,   // center latitude
                2.3522,    // center longitude
                50_000,    // bias radius in metres
                "fr"       // language
        );
    }

    // Place details — returns coordinates, phone, rating, Maps URL
    public PlaceAddress details(String placeId) {
        return googlePlacesService.getPlace(placeId, "fr");
    }

    // Nearby search — returns up to 20 POIs of the given types
    public List<PlaceAddress> nearby(double lat, double lng) {
        return googlePlacesService.searchNearby(
                lat, lng,
                10_000,                          // radius in metres
                List.of("gas_station"),          // place types
                "fr",                            // language
                20                               // max results (1–20)
        );
    }
}
```

---

## Model

### `PlaceSuggestion` — autocomplete result

| Field | Type | Description |
|---|---|---|
| `placeId` | `String` | Google Place ID |
| `mainText` | `String` | Short display name |
| `fullText` | `String` | Full formatted text |
| `distanceMeters` | `Integer` | Distance from bias centre, or `null` |

### `PlaceAddress` — place details / nearby result

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Google Place ID |
| `name` | `String` | Display name |
| `formattedAddress` | `String` | Full formatted address |
| `lat` | `Double` | Latitude |
| `lng` | `Double` | Longitude |
| `googleMapsUri` | `String` | Google Maps URL |
| `phoneNumber` | `String` | International phone number |
| `rating` | `Double` | Rating out of 5, or `null` |
| `types` | `List<String>` | Place type strings |

---

## How it works

Each service call creates a `PlacesGrpc.PlacesBlockingStub` from the Quarkus-managed `Channel` (`@GrpcClient("google-places")`).  
A `GoogleApiKeyInterceptor` is applied per call to inject two gRPC metadata headers:

| Header | Value |
|---|---|
| `x-goog-api-key` | API key from `GoogleApiKeySupplier.getApiKey()` |
| `x-goog-fieldmask` | Comma-separated field mask (only for `GetPlace` and `SearchNearby`) |

---

## License

Apache License 2.0
