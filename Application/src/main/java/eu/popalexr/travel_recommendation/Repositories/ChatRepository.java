package eu.popalexr.travel_recommendation.Repositories;

import eu.popalexr.travel_recommendation.Models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserIdOrderByIdDesc(Long userId);

    Optional<Chat> findByIdAndUserId(Long id, Long userId);
}

