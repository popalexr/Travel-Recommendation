package eu.popalexr.travel_recommendation.Services;

import eu.popalexr.travel_recommendation.Models.ChatMessage;

import java.util.List;

public interface OpenAiChatService {

    /**
     * Sends the given chat history to the OpenAI Chat Completions API
     * and returns the assistant reply as an HTML snippet
     * (no outer &lt;html&gt; or &lt;body&gt; tags).
     */
    String chat(List<ChatMessage> messages);

    /**
     * Generates a short, human-readable title for a chat,
     * based on the first user message and (optionally) the first assistant reply.
     */
    String generateTitle(String firstUserMessage, String assistantReply);
}
