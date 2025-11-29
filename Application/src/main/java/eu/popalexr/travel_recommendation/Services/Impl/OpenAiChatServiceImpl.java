package eu.popalexr.travel_recommendation.Services.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Services.OpenAiChatService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        requireApiKey();

        try {
            ObjectNode root = baseChatRequest();
            ArrayNode apiMessages = (ArrayNode) root.get("messages");

            appendHistory(apiMessages, messages);

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
        requireApiKey();

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

    @Override
    public String analyzeTicket(List<ChatMessage> messages, String fileName, byte[] fileBytes, String contentType) {
        requireApiKey();

        try {
            return analyzeDocument(
                "You are a travel assistant that reads airline tickets, boarding passes, and flight confirmations. "
                    + "Extract structured details: passenger name, airline, booking reference, flight number(s), "
                    + "departure and arrival airport names and IATA codes, terminals/gates, dates, times, seat, baggage, "
                    + "layovers, and notable rules. "
                    + "Respond concisely using HTML only. Use short headings and bullet lists. "
                    + "If a field is missing, state 'not provided' rather than guessing.",
                messages,
                "Please analyze this uploaded airplane ticket/boarding pass and summarize the travel details and constraints in HTML.",
                "Ticket PDF text (truncated):\n",
                contentType,
                fileBytes
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze ticket with OpenAI API", e);
        }
    }

    @Override
    public String analyzeAccommodation(List<ChatMessage> messages, String fileName, byte[] fileBytes, String contentType) {
        requireApiKey();

        try {
            return analyzeDocument(
                "You are a travel assistant that reads accommodation invoices and booking confirmations. "
                    + "Extract structured details: guest name, property name, address, booking/confirmation number, "
                    + "check-in and check-out dates/times, number of guests, room type, nightly rate and currency, "
                    + "total cost with taxes/fees, included meals (e.g., breakfast), cancellation policy, payment status, "
                    + "contact details, and special notes or restrictions. "
                    + "Respond concisely using HTML only. Use short headings and bullet lists. "
                    + "If a field is missing, state 'not provided' rather than guessing.",
                messages,
                "Please analyze this uploaded accommodation invoice/booking confirmation and summarize the stay details and constraints in HTML.",
                "Accommodation PDF text (truncated):\n",
                contentType,
                fileBytes
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze accommodation invoice with OpenAI API", e);
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

    private void appendHistory(ArrayNode apiMessages, List<ChatMessage> messages) {
        if (messages == null) {
            return;
        }
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            ObjectNode node = apiMessages.addObject();
            node.put("role", message.getRole());
            node.put("content", message.getText() == null ? "" : message.getText());
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }
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

    private String analyzeDocument(
        String systemPrompt,
        List<ChatMessage> messages,
        String userIntroText,
        String pdfPrefix,
        String contentType,
        byte[] fileBytes
    ) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ArrayNode apiMessages = root.putArray("messages");

        ObjectNode systemMessage = apiMessages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        appendHistory(apiMessages, messages);

        ObjectNode userMessage = apiMessages.addObject();
        userMessage.put("role", "user");
        ArrayNode contentArray = userMessage.putArray("content");

        contentArray.addObject()
            .put("type", "text")
            .put("text", userIntroText);

        if (contentType != null && contentType.startsWith("image/")) {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
            ObjectNode imageNode = contentArray.addObject();
            imageNode.put("type", "image_url");
            ObjectNode imageUrl = imageNode.putObject("image_url");
            imageUrl.put("url", dataUrl);
            imageUrl.put("detail", "high");
        } else if ("application/pdf".equalsIgnoreCase(contentType)) {
            String text = extractPdfText(fileBytes, pdfPrefix);
            contentArray.addObject()
                .put("type", "text")
                .put("text", text);
        } else {
            String fallback = Base64.getEncoder().encodeToString(fileBytes);
            contentArray.addObject()
                .put("type", "text")
                .put("text", "Unknown file type (" + contentType + "). Base64 payload:\n" + fallback);
        }

        JsonNode contentNode = executeChat(root);
        if (contentNode == null || contentNode.isNull()) {
            return "The document could not be interpreted.";
        }

        return contentNode.asText();
    }

    private String extractPdfText(byte[] bytes, String prefix) {
        if (bytes == null || bytes.length == 0) {
            return "No PDF content provided.";
        }

        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null) {
                return "PDF text could not be extracted.";
            }
            text = text.trim();
            if (text.length() > 8000) {
                text = text.substring(0, 8000);
            }
            if (text.isEmpty()) {
                return "PDF text could not be extracted.";
            }
            return (prefix == null ? "" : prefix) + text;
        } catch (IOException e) {
            return "Unable to extract text from PDF. Please rely on the image or provide key details manually.";
        }
    }
}
