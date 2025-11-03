package eu.popalexr.travel_recommendation.Repositories;

import eu.popalexr.travel_recommendation.Models.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
}

