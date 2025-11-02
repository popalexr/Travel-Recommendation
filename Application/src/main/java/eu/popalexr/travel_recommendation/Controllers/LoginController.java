package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.DTOs.LoginRequest;
import eu.popalexr.travel_recommendation.Services.UserService;
import io.github.inertia4j.spring.Inertia;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Controller
public class LoginController {

    private final Inertia inertia;
    private final UserService userService;

    public LoginController(Inertia inertia, UserService userService) {
        this.inertia = inertia;
        this.userService = userService;
    }

    @GetMapping("/login")
    public ResponseEntity<String> index() {
        return this.inertia.render("Auth", Map.of("initialTab", "login"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(
        @Valid @RequestBody LoginRequest request,
        HttpSession session
    ) {
        var user = userService.authenticate(request);
        session.setAttribute(SessionConstants.AUTHENTICATED_USER_ID, user.getId());

        return ResponseEntity.ok(Map.of(
            "message", "Login successful.",
            "userId", user.getId(),
            "sessionToken", session.getId()
        ));
    }
}
