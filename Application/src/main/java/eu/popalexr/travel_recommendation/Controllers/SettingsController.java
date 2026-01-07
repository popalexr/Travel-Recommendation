package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import io.github.inertia4j.spring.Inertia;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SettingsController {

    private final Inertia inertia;
    private final UserRepository userRepository;

    public SettingsController(Inertia inertia, UserRepository userRepository) {
        this.inertia = inertia;
        this.userRepository = userRepository;
    }

    @GetMapping("/settings")
    public ResponseEntity<String> settings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);

        return userRepository.findById(userId)
            .map(user -> {
                Map<String, Object> userPayload = new HashMap<>();
                userPayload.put("id", user.getId());
                userPayload.put("email", user.getEmail());
                userPayload.put("firstName", user.getFirstName());
                userPayload.put("lastName", user.getLastName());
                return this.inertia.render("Settings", Map.of("user", userPayload));
            })
            .orElseGet(() -> this.inertia.render("Settings", Map.of()));
    }
}
