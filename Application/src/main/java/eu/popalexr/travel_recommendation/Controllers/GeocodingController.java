package eu.popalexr.travel_recommendation.Controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
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
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeocodingController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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
        String url = NOMINATIM_URL
            + "?format=json"
            + "&limit=1"
            + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", "Travel-Recommendation/1.0 (contact: local-dev)")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray() || root.isEmpty()) {
            return null;
        }

        JsonNode first = root.get(0);
        if (first == null) {
            return null;
        }

        double lat = first.path("lat").asDouble(Double.NaN);
        double lng = first.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            return null;
        }

        String displayName = first.path("display_name").asText(query);
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
