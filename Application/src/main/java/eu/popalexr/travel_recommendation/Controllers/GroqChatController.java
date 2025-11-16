package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Models.Chat;
import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Models.User;
import eu.popalexr.travel_recommendation.Repositories.ChatMessageRepository;
import eu.popalexr.travel_recommendation.Repositories.ChatRepository;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import eu.popalexr.travel_recommendation.Services.GroqChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class GroqChatController {

    private final GroqChatService groqChatService;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public GroqChatController(
        GroqChatService groqChatService,
        ChatRepository chatRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository
    ) {
        this.groqChatService = groqChatService;
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public static class ChatRequest {
        private Long chatId;
        private String message;

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/api/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Message is required.")
            );
        }

        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "User not found.")
                );
            }
            User user = userOpt.get();

            Chat chat;
            boolean isNewChat = request.getChatId() == null;

            if (isNewChat) {
                chat = Chat.create(user, null);
                chat = chatRepository.save(chat);
            } else {
                chat = chatRepository.findByIdAndUserId(request.getChatId(), userId)
                    .orElse(null);
                if (chat == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", "Chat not found.")
                    );
                }
            }

            String userMessageText = request.getMessage().trim();
            ChatMessage userMessage = ChatMessage.create(chat, "user", userMessageText);
            chatMessageRepository.save(userMessage);

            List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            String reply = groqChatService.chat(history);
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", reply);
            chatMessageRepository.save(assistantMessageEntity);

            if (isNewChat) {
                String title = groqChatService.generateTitle(userMessageText, reply);
                chat.setTitle(title);
                chatRepository.save(chat);
            }

            Map<String, Object> assistantMessage = Map.of(
                "id", "assistant-" + System.currentTimeMillis(),
                "role", "assistant",
                "content", reply,
                "timestamp", OffsetDateTime.now().toString()
            );

            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "chatTitle", chat.getTitle(),
                    "message", assistantMessage
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Groq API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to contact the recommendation engine.")
            );
        }
    }

    @GetMapping("/api/chat/{id}/messages")
    public ResponseEntity<Map<String, Object>> getChatMessages(
        @PathVariable("id") Long chatId,
        HttpServletRequest httpRequest
    ) {
        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        Optional<Chat> chatOpt = chatRepository.findByIdAndUserId(chatId, userId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Chat not found.")
            );
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByIdAsc(chatId);
        List<Map<String, Object>> dto = messages.stream()
            .map(msg -> Map.<String, Object>of(
                "id", msg.getId(),
                "role", msg.getRole(),
                "content", msg.getText(),
                "timestamp", ""
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("messages", dto));
    }
}
