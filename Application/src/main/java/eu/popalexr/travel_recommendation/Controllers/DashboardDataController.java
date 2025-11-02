package eu.popalexr.travel_recommendation.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DashboardDataController {

    @GetMapping("/api/dashboard")
    public ResponseEntity<Map<String, Object>> data() {
        return ResponseEntity.ok(
            Map.of(
                "previousRecommendations", List.of(),
                "chatMessages", List.of()
            )
        );
    }
}
