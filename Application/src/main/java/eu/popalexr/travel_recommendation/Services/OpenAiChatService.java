package eu.popalexr.travel_recommendation.Services;

import eu.popalexr.travel_recommendation.Models.ChatMessage;
import eu.popalexr.travel_recommendation.Models.TripProfile;

import java.util.List;
import java.util.function.Consumer;

public interface OpenAiChatService {

    /**
     * Sends the given chat history to the OpenAI Chat Completions API
     * and returns the assistant reply as an HTML snippet
     * (no outer &lt;html&gt; or &lt;body&gt; tags).
     */
    String chat(List<ChatMessage> messages, TripProfile profile);

    /**
     * Streams the assistant reply. The provided consumer is invoked with each incremental
     * text delta. The full concatenated reply is returned once streaming completes.
     */
    String streamChat(List<ChatMessage> messages, TripProfile profile, Consumer<String> onDelta);

    /**
     * Generates a short, human-readable title for a chat,
     * based on the first user message and (optionally) the first assistant reply.
     */
    String generateTitle(String firstUserMessage, String assistantReply);

    /**
     * Reads an uploaded airplane ticket (PDF or image), extracts the travel details,
     * and returns a concise HTML summary that can be shown inside the chat feed.
     */
    String analyzeTicket(List<ChatMessage> messages, String fileName, byte[] fileBytes, String contentType);

    /**
     * Reads an uploaded accommodation invoice/booking confirmation (PDF or image),
     * extracts stay details, and returns a concise HTML summary suitable for the chat feed.
     */
    String analyzeAccommodation(List<ChatMessage> messages, String fileName, byte[] fileBytes, String contentType);

    /**
     * Reads an uploaded travel-related document (PDF or image),
     * extracts relevant details, and returns a concise HTML summary suitable for the chat feed.
     */
    String analyzeOtherDocument(List<ChatMessage> messages, String fileName, byte[] fileBytes, String contentType);

    /**
     * Extracts itinerary day data from an assistant response and returns JSON (string).
     * Returns null if the extraction fails or no itinerary exists.
     */
    String extractItineraryJson(String assistantResponse);
}
