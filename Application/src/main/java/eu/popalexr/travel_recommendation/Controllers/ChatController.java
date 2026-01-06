package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Models.Chat;
import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Models.TripProfile;
import eu.popalexr.travel_recommendation.Models.User;
import eu.popalexr.travel_recommendation.Repositories.ChatMessageRepository;
import eu.popalexr.travel_recommendation.Repositories.ChatRepository;
import eu.popalexr.travel_recommendation.Repositories.TripProfileRepository;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import eu.popalexr.travel_recommendation.Services.OpenAiChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class ChatController {

    private static final Pattern CODE_FENCE_PATTERN =
        Pattern.compile("^```(?:\\w+)?\\s*([\\s\\S]*?)\\s*```$", Pattern.DOTALL);

    private final OpenAiChatService chatService;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final TripProfileRepository tripProfileRepository;

    public ChatController(
        OpenAiChatService chatService,
        ChatRepository chatRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository,
        TripProfileRepository tripProfileRepository
    ) {
        this.chatService = chatService;
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.tripProfileRepository = tripProfileRepository;
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

    public static class EditMessageRequest {
        private Long chatId;
        private Long messageId;
        private String message;

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
        }

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class RegenerateRequest {
        private Long chatId;

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
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
            TripProfile profile = tripProfileRepository.findByChatId(chat.getId()).orElse(null);
            String reply = stripCodeFences(chatService.chat(history, profile));
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", reply);
            chatMessageRepository.save(assistantMessageEntity);

            if (isNewChat) {
                String title = chatService.generateTitle(userMessageText, reply);
                chat.setTitle(title);
                chatRepository.save(chat);
            }

            Map<String, Object> userDto = messageDto(userMessage);
            Map<String, Object> assistantMessage = messageDto(assistantMessageEntity);

            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "chatTitle", chat.getTitle(),
                    "message", assistantMessage,
                    "messages", List.of(userDto, assistantMessage)
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "OpenAI API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to contact the recommendation engine.")
            );
        }
    }

    @PostMapping(value = "/api/chat/upload-ticket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadTicket(
        @RequestParam(value = "chatId", required = false) Long chatId,
        @RequestPart("file") MultipartFile file,
        HttpServletRequest httpRequest
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "A ticket file is required.")
            );
        }

        String contentType = file.getContentType();
        boolean supported = contentType != null
            && (contentType.startsWith("image/") || contentType.equalsIgnoreCase("application/pdf"));
        if (!supported) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Only PDF or image files are supported.")
            );
        }

        final long maxSizeBytes = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "File too large. Please upload files up to 10MB.")
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
            boolean isNewChat = chatId == null;

            if (isNewChat) {
                chat = Chat.create(user, null);
                chat = chatRepository.save(chat);
            } else {
                chat = chatRepository.findByIdAndUserId(chatId, userId).orElse(null);
                if (chat == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", "Chat not found.")
                    );
                }
            }

            String fileName = file.getOriginalFilename() == null ? "ticket" : file.getOriginalFilename();
            String userMessageText = "Uploaded airplane ticket: " + fileName;
            ChatMessage userMessage = ChatMessage.create(chat, "user", userMessageText);
            chatMessageRepository.save(userMessage);

            List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            String reply = chatService.analyzeTicket(history, fileName, file.getBytes(), contentType);
            String cleanedReply = stripCodeFences(reply);
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", cleanedReply);
            chatMessageRepository.save(assistantMessageEntity);

            if (isNewChat) {
                String title = chatService.generateTitle(userMessageText, reply);
                chat.setTitle(title);
                chatRepository.save(chat);
            }

            Map<String, Object> userDto = Map.of(
                "id", userMessage.getId(),
                "role", "user",
                "content", userMessageText,
                "timestamp", OffsetDateTime.now().toString()
            );

            Map<String, Object> assistantDto = Map.of(
                "id", assistantMessageEntity.getId(),
                "role", "assistant",
                "content", assistantMessageEntity.getText(),
                "timestamp", OffsetDateTime.now().toString()
            );

            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "chatTitle", chat.getTitle(),
                    "messages", List.of(userDto, assistantDto)
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "OpenAI API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to process the ticket. Please try again.")
            );
        }
    }

    @PostMapping(value = "/api/chat/upload-accommodation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAccommodation(
        @RequestParam(value = "chatId", required = false) Long chatId,
        @RequestPart("file") MultipartFile file,
        HttpServletRequest httpRequest
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "An accommodation invoice or booking file is required.")
            );
        }

        String contentType = file.getContentType();
        boolean supported = contentType != null
            && (contentType.startsWith("image/") || contentType.equalsIgnoreCase("application/pdf"));
        if (!supported) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Only PDF or image files are supported.")
            );
        }

        final long maxSizeBytes = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "File too large. Please upload files up to 10MB.")
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
            boolean isNewChat = chatId == null;

            if (isNewChat) {
                chat = Chat.create(user, null);
                chat = chatRepository.save(chat);
            } else {
                chat = chatRepository.findByIdAndUserId(chatId, userId).orElse(null);
                if (chat == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", "Chat not found.")
                    );
                }
            }

            String fileName = file.getOriginalFilename() == null ? "accommodation" : file.getOriginalFilename();
            String userMessageText = "Uploaded accommodation invoice: " + fileName;
            ChatMessage userMessage = ChatMessage.create(chat, "user", userMessageText);
            chatMessageRepository.save(userMessage);

            List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            String reply = chatService.analyzeAccommodation(history, fileName, file.getBytes(), contentType);
            String cleanedReply = stripCodeFences(reply);
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", cleanedReply);
            chatMessageRepository.save(assistantMessageEntity);

            if (isNewChat) {
                String title = chatService.generateTitle(userMessageText, reply);
                chat.setTitle(title);
                chatRepository.save(chat);
            }

            Map<String, Object> userDto = Map.of(
                "id", userMessage.getId(),
                "role", "user",
                "content", userMessageText,
                "timestamp", OffsetDateTime.now().toString()
            );

            Map<String, Object> assistantDto = Map.of(
                "id", assistantMessageEntity.getId(),
                "role", "assistant",
                "content", assistantMessageEntity.getText(),
                "timestamp", OffsetDateTime.now().toString()
            );

            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "chatTitle", chat.getTitle(),
                    "messages", List.of(userDto, assistantDto)
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "OpenAI API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to process the accommodation document. Please try again.")
            );
        }
    }

    @PostMapping("/api/chat/edit-latest")
    public ResponseEntity<Map<String, Object>> editLatestMessage(
        @RequestBody EditMessageRequest request,
        HttpServletRequest httpRequest
    ) {
        if (request == null || request.getChatId() == null || request.getMessageId() == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Chat and message IDs are required.")
            );
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Message content is required.")
            );
        }

        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        Chat chat = chatRepository.findByIdAndUserId(request.getChatId(), userId).orElse(null);
        if (chat == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Chat not found.")
            );
        }

        try {
            List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            int lastUserIndex = findLastUserMessageIndex(history);
            if (lastUserIndex < 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "No user message found to edit.")
                );
            }

            ChatMessage lastUserMessage = history.get(lastUserIndex);
            if (!lastUserMessage.getId().equals(request.getMessageId())) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Only the latest user message can be edited.")
                );
            }
            if (isUploadMessage(lastUserMessage.getText())) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Editing uploaded documents is not supported.")
                );
            }

            lastUserMessage.setText(request.getMessage().trim());
            chatMessageRepository.save(lastUserMessage);

            deleteMessagesAfterIndex(history, lastUserIndex);

            List<ChatMessage> regenHistory = new ArrayList<>(history.subList(0, lastUserIndex + 1));
            TripProfile profile = tripProfileRepository.findByChatId(chat.getId()).orElse(null);
            String reply = stripCodeFences(chatService.chat(regenHistory, profile));
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", reply);
            chatMessageRepository.save(assistantMessageEntity);

            List<ChatMessage> updated = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "messages", buildMessageDtos(updated)
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "OpenAI API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to regenerate the recommendation.")
            );
        }
    }

    @PostMapping("/api/chat/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateRecommendation(
        @RequestBody RegenerateRequest request,
        HttpServletRequest httpRequest
    ) {
        if (request == null || request.getChatId() == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Chat ID is required.")
            );
        }

        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        Chat chat = chatRepository.findByIdAndUserId(request.getChatId(), userId).orElse(null);
        if (chat == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Chat not found.")
            );
        }

        try {
            List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            int lastUserIndex = findLastUserMessageIndex(history);
            if (lastUserIndex < 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "No user message found to regenerate.")
                );
            }

            ChatMessage lastUserMessage = history.get(lastUserIndex);
            if (isUploadMessage(lastUserMessage.getText())) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Regeneration is not available for uploaded documents.")
                );
            }

            deleteMessagesAfterIndex(history, lastUserIndex);

            List<ChatMessage> regenHistory = new ArrayList<>(history.subList(0, lastUserIndex + 1));
            TripProfile profile = tripProfileRepository.findByChatId(chat.getId()).orElse(null);
            String reply = stripCodeFences(chatService.chat(regenHistory, profile));
            ChatMessage assistantMessageEntity = ChatMessage.create(chat, "assistant", reply);
            chatMessageRepository.save(assistantMessageEntity);

            List<ChatMessage> updated = chatMessageRepository.findByChatIdOrderByIdAsc(chat.getId());
            return ResponseEntity.ok(
                Map.of(
                    "chatId", chat.getId(),
                    "messages", buildMessageDtos(updated)
                )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "OpenAI API key is not configured on the server.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Failed to regenerate the recommendation.")
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
                "content", "assistant".equals(msg.getRole())
                    ? stripCodeFences(msg.getText())
                    : msg.getText(),
                "timestamp", ""
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("messages", dto));
    }

    private List<Map<String, Object>> buildMessageDtos(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
            .map(this::messageDto)
            .collect(Collectors.toList());
    }

    private Map<String, Object> messageDto(ChatMessage message) {
        String content = message.getText();
        if ("assistant".equals(message.getRole())) {
            content = stripCodeFences(content);
        }
        return Map.of(
            "id", message.getId(),
            "role", message.getRole(),
            "content", content,
            "timestamp", ""
        );
    }

    private int findLastUserMessageIndex(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return -1;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && "user".equals(message.getRole())) {
                return i;
            }
        }
        return -1;
    }

    private void deleteMessagesAfterIndex(List<ChatMessage> messages, int lastUserIndex) {
        if (messages == null || lastUserIndex < 0) {
            return;
        }
        for (int i = messages.size() - 1; i > lastUserIndex; i--) {
            ChatMessage message = messages.get(i);
            if (message != null) {
                chatMessageRepository.delete(message);
            }
        }
    }

    private boolean isUploadMessage(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase();
        return normalized.startsWith("uploaded airplane ticket:")
            || normalized.startsWith("uploaded accommodation invoice:");
    }

    private String stripCodeFences(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        Matcher matcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return content;
    }
}
