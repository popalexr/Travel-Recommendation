package eu.popalexr.travel_recommendation.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @Column(length = 50)
    private String id; // jti

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected AuthSession() {
    }

    private AuthSession(String id, Long userId, LocalDateTime expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public static AuthSession create(String id, Long userId, LocalDateTime expiresAt) {
        return new AuthSession(id, userId, expiresAt);
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(LocalDateTime when) {
        this.revokedAt = when == null ? LocalDateTime.now() : when;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

