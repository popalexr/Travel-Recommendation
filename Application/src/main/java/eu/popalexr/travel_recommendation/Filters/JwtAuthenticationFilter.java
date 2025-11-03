package eu.popalexr.travel_recommendation.Filters;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Security.JwtService;
import eu.popalexr.travel_recommendation.Repositories.AuthSessionRepository;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@Order(0)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthSessionRepository sessionRepository;
    private final String cookieName;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AuthSessionRepository sessionRepository,
                                   @Value("${jwt.cookie-name:AUTH_TOKEN}") String cookieName) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Try Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");
        Optional<String> tokenOpt = Optional.empty();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenOpt = Optional.of(authHeader.substring(7));
        }

        // Fallback to cookie
        if (tokenOpt.isEmpty()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                tokenOpt = Arrays.stream(cookies)
                    .filter(c -> cookieName.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
            }
        }

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            jwtService.validate(token).ifPresent(decoded -> {
                Long uid = decoded.getClaim("uid").asLong();
                String jti = safeJti(decoded);
                if (uid != null && jti != null) {
                    sessionRepository.findById(jti).ifPresent(session -> {
                        if (session.getUserId().equals(uid) && session.isActive(java.time.LocalDateTime.now())) {
                            request.setAttribute(SessionConstants.AUTHENTICATED_USER_ID, uid);
                            // also expose session id for logout
                            request.setAttribute("AUTH_SESSION_ID", jti);
                        }
                    });
                }
            });
        }

        filterChain.doFilter(request, response);
    }

    private String safeJti(DecodedJWT decoded) {
        try {
            String jti = decoded.getId();
            return (jti == null || jti.isBlank()) ? null : jti;
        } catch (Exception e) {
            return null;
        }
    }
}
