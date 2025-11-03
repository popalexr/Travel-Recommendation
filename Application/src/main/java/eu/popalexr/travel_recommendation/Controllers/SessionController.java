package eu.popalexr.travel_recommendation.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import eu.popalexr.travel_recommendation.Repositories.AuthSessionRepository;
import eu.popalexr.travel_recommendation.Security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class SessionController {

    private final String jwtCookieName;
    private final AuthSessionRepository sessionRepository;
    private final JwtService jwtService;

    public SessionController(@Value("${jwt.cookie-name:AUTH_TOKEN}") String jwtCookieName,
                             AuthSessionRepository sessionRepository,
                             JwtService jwtService) {
        this.jwtCookieName = jwtCookieName;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        // Attempt to revoke the session in DB when logging out
        String token = null;
        var cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (jwtCookieName.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token != null && !token.isBlank()) {
            jwtService.validate(token).ifPresent(decoded -> {
                String jti = decoded.getId();
                if (jti != null) {
                    sessionRepository.findById(jti).ifPresent(s -> {
                        s.revoke(LocalDateTime.now());
                        sessionRepository.save(s);
                    });
                }
            });
        }
        Cookie cookie = new Cookie(jwtCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false); // consider true behind HTTPS
        cookie.setMaxAge(0); // delete cookie
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully."
        ));
    }
}
