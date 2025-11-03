package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.DTOs.LoginRequest;
import eu.popalexr.travel_recommendation.Services.UserService;
import io.github.inertia4j.spring.Inertia;
import eu.popalexr.travel_recommendation.Security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import eu.popalexr.travel_recommendation.Models.AuthSession;
import eu.popalexr.travel_recommendation.Repositories.AuthSessionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
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
    private final JwtService jwtService;
    private final String jwtCookieName;
    private final AuthSessionRepository sessionRepository;

    public LoginController(Inertia inertia, UserService userService, JwtService jwtService,
                           @Value("${jwt.cookie-name:AUTH_TOKEN}") String jwtCookieName,
                           AuthSessionRepository sessionRepository) {
        this.inertia = inertia;
        this.userService = userService;
        this.jwtService = jwtService;
        this.jwtCookieName = jwtCookieName;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/login")
    public ResponseEntity<String> index() {
        return this.inertia.render("Auth", Map.of("initialTab", "login"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        var user = userService.authenticate(request);

        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(getExpirationSeconds());
        sessionRepository.save(AuthSession.create(sessionId, user.getId(), expiresAt));

        String token = jwtService.generateToken(user.getId(), sessionId);
        Cookie cookie = new Cookie(jwtCookieName, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false); // consider true behind HTTPS
        cookie.setMaxAge((int) getExpirationSeconds());
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
            "message", "Login successful.",
            "userId", user.getId(),
            "sessionToken", token
        ));
    }

    @Value("${jwt.expiration-seconds:2592000}")
    private long expirationSeconds;

    private long getExpirationSeconds() {
        return expirationSeconds;
    }
}
