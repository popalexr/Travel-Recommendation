package eu.popalexr.travel_recommendation.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Lob
    @Column(name = "text", nullable = false, columnDefinition = "LONGTEXT")
    private String text;

    protected ChatMessage() {
        // for JPA
    }

    private ChatMessage(Chat chat, String role, String text) {
        this.chat = chat;
        this.role = role;
        this.text = text;
    }

    public static ChatMessage create(Chat chat, String role, String text) {
        return new ChatMessage(chat, role, text);
    }

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
