package eu.popalexr.travel_recommendation.Controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class GeocodingController {

    private static final int MAX_LOCATIONS = 8;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String mapboxToken;

    public GeocodingController(
        ObjectMapper objectMapper,
        @Value("${mapbox.api-key:}") String mapboxToken
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.mapboxToken = mapboxToken;
    }

    public static class GeocodeRequest {
        private List<String> locations;

        public List<String> getLocations() {
            return locations;
        }

        public void setLocations(List<String> locations) {
            this.locations = locations;
        }
    }

    @PostMapping("/api/geocode")
    public ResponseEntity<Map<String, Object>> geocode(
        @RequestBody GeocodeRequest request,
        HttpServletRequest httpRequest
    ) {
        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        if (mapboxToken == null || mapboxToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Mapbox API key is not configured.")
            );
        }

        List<String> locations = request == null ? null : request.getLocations();
        if (locations == null || locations.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Locations are required.")
            );
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int count = 0;
        for (String location : locations) {
            if (count >= MAX_LOCATIONS) {
                break;
            }
            String trimmed = normalize(location);
            if (trimmed == null) {
                continue;
            }
            try {
                Map<String, Object> result = geocodeSingle(trimmed);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                // Skip failed lookups to keep the response usable.
            }
            count++;
        }

        return ResponseEntity.ok(Map.of("results", results));
    }

    private Map<String, Object> geocodeSingle(String query) throws Exception {
        String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
            + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + ".json?limit=1&access_token=" + URLEncoder.encode(mapboxToken, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode features = root.path("features");
        if (!features.isArray() || features.isEmpty()) {
            return null;
        }

        JsonNode first = features.get(0);
        if (first == null) {
            return null;
        }

        JsonNode center = first.path("center");
        if (!center.isArray() || center.size() < 2) {
            return null;
        }
        double lng = center.get(0).asDouble(Double.NaN);
        double lat = center.get(1).asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            return null;
        }

        String displayName = first.path("place_name").asText(query);
        return Map.of(
            "query", query,
            "lat", lat,
            "lng", lng,
            "displayName", displayName
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
