package eu.popalexr.travel_recommendation.Security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expirationSeconds;

    public JwtService(
        @Value("${jwt.secret:change-me-in-prod}") String secret,
        @Value("${jwt.issuer:travel-recommendation}") String issuer,
        @Value("${jwt.expiration-seconds:2592000}") long expirationSeconds // default 30 days
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(this.algorithm).withIssuer(issuer).build();
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer(issuer)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expirationSeconds)))
            .withClaim("uid", userId)
            .sign(algorithm);
    }

    public String generateToken(Long userId, String sessionId) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer(issuer)
            .withJWTId(sessionId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expirationSeconds)))
            .withClaim("uid", userId)
            .sign(algorithm);
    }

    public Optional<Long> validateAndExtractUserId(String token) {
        try {
            DecodedJWT decoded = verifier.verify(token);
            Long uid = decoded.getClaim("uid").asLong();
            return Optional.ofNullable(uid);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<DecodedJWT> validate(String token) {
        try {
            return Optional.of(verifier.verify(token));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
