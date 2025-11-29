package eu.popalexr.travel_recommendation.Services.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Services.OpenAiChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class OpenAiChatServiceImpl implements OpenAiChatService {

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiChatServiceImpl(
        @Value("${openai.api-key:}") String apiKey,
        @Value("${openai.model:gpt-4o-mini}") String model,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        try {
            ObjectNode root = baseChatRequest();
            ArrayNode apiMessages = (ArrayNode) root.get("messages");

            if (messages != null) {
                for (ChatMessage message : messages) {
                    if (message == null) {
                        continue;
                    }
                    ObjectNode node = apiMessages.addObject();
                    node.put("role", message.getRole());
                    node.put("content", message.getText());
                }
            }

            JsonNode contentNode = executeChat(root);
            if (contentNode == null || contentNode.isNull()) {
                return "The recommendation engine did not return any content.";
            }

            return contentNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    @Override
    public String generateTitle(String firstUserMessage, String assistantReply) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);

            ArrayNode messages = root.putArray("messages");

            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                "You generate very short, descriptive titles for travel planning chats. "
                    + "Respond with ONLY the title, no quotes, maximum 60 characters.");

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            StringBuilder sb = new StringBuilder();
            sb.append("First user message: ").append(firstUserMessage);
            if (assistantReply != null && !assistantReply.isBlank()) {
                sb.append("\nAssistant reply: ").append(assistantReply);
            }
            userMessage.put("content", sb.toString());

            JsonNode contentNode = executeChat(root);
            if (contentNode == null || contentNode.isNull()) {
                return "New travel chat";
            }

            String title = contentNode.asText().trim();
            if (title.length() > 60) {
                title = title.substring(0, 60).trim();
            }
            if (title.isEmpty()) {
                return "New travel chat";
            }
            return title;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate chat title with OpenAI API", e);
        }
    }

    private ObjectNode baseChatRequest() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put(
            "content",
            "You are a helpful travel recommendation assistant. "
                + "Answer concisely and structure your reply using HTML only (no Markdown). "
                + "Use semantic HTML elements like <p>, <ul>, <ol>, <li>, <h2>, and <strong> where appropriate. "
                + "Return only an HTML snippet without enclosing <html> or <body> tags."
        );

        return root;
    }

    private JsonNode executeChat(ObjectNode root) throws Exception {
        String requestBody = objectMapper.writeValueAsString(root);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            String responseBody = response.body();
            String errorDetails = responseBody == null || responseBody.isBlank() ? "" : (": " + responseBody);
            throw new RuntimeException("OpenAI API returned status " + response.statusCode() + errorDetails);
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("choices").path(0).path("message").path("content");
    }
}
