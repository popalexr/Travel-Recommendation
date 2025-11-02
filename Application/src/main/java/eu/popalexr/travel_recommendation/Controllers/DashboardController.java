package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import io.github.inertia4j.spring.Inertia;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class DashboardController {

    private final Inertia inertia;
    private final UserRepository userRepository;

    public DashboardController(Inertia inertia, UserRepository userRepository) {
        this.inertia = inertia;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);

        return userRepository.findById(userId)
            .map(user -> this.inertia.render("Dashboard", Map.of(
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName()
                )
            )))
            .orElseGet(() -> this.inertia.render("Dashboard", Map.of()));
    }
}
