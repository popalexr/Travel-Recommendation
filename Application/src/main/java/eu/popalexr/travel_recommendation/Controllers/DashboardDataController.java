package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Models.Chat;
import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Repositories.ChatMessageRepository;
import eu.popalexr.travel_recommendation.Repositories.ChatRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class DashboardDataController {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DashboardDataController(ChatRepository chatRepository, ChatMessageRepository chatMessageRepository) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<Map<String, Object>> data(HttpServletRequest request) {
        Object uid = request.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.ok(
                Map.of(
                    "previousRecommendations", List.of(),
                    "chatMessages", List.of()
                )
            );
        }

        List<Chat> chats = chatRepository.findByUserIdOrderByIdDesc(userId);

        List<Map<String, Object>> previousRecommendations = chats.stream()
            .map(chat -> Map.<String, Object>of(
                "id", chat.getId(),
                "title", chat.getTitle() == null || chat.getTitle().isBlank() ? "Untitled chat" : chat.getTitle(),
                "subtitle", "AI travel recommendations"
            ))
            .collect(Collectors.toList());

        List<Map<String, Object>> chatMessages;
        if (chats.isEmpty()) {
            chatMessages = Collections.emptyList();
        } else {
            Chat latest = chats.getFirst();
            List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByIdAsc(latest.getId());
            chatMessages = messages.stream()
                .map(msg -> {
                    Map<String, Object> dto = new java.util.HashMap<>();
                    String content = "assistant".equals(msg.getRole())
                        ? stripCodeFences(msg.getText())
                        : msg.getText();
                    dto.put("id", msg.getId());
                    dto.put("role", msg.getRole());
                    dto.put("content", content);
                    dto.put("timestamp", "");
                    if (msg.getItineraryJson() != null && !msg.getItineraryJson().isBlank()) {
                        dto.put("itinerary", msg.getItineraryJson());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
            Map.of(
                "previousRecommendations", previousRecommendations,
                "chatMessages", chatMessages
            )
        );
    }

    private String stripCodeFences(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            String inner = trimmed.substring(3, trimmed.length() - 3).trim();
            if (inner.startsWith("html")) {
                inner = inner.substring(4).trim();
            }
            return inner;
        }
        return content;
    }
}
