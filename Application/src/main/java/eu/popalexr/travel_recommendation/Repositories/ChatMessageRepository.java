package eu.popalexr.travel_recommendation.Repositories;

import eu.popalexr.travel_recommendation.Models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatIdOrderByIdAsc(Long chatId);
}

