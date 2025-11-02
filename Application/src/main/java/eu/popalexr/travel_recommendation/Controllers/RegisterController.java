package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.DTOs.RegisterRequest;
import eu.popalexr.travel_recommendation.Services.UserService;
import io.github.inertia4j.spring.Inertia;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Controller
public class RegisterController {

    private final Inertia inertia;
    private final UserService userService;

    public RegisterController(Inertia inertia, UserService userService) {
        this.inertia = inertia;
        this.userService = userService;
    }

    @GetMapping("/register")
    public ResponseEntity<String> index() {
        return this.inertia.render("Auth", Map.of("initialTab", "register"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpSession session
    ) {
        var user = userService.register(request);
        session.setAttribute(SessionConstants.AUTHENTICATED_USER_ID, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Registration successful.",
            "userId", user.getId(),
            "sessionToken", session.getId()
        ));
    }
}
