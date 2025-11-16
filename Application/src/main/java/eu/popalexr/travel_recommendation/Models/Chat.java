package eu.popalexr.travel_recommendation.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 255)
    private String title;

    protected Chat() {
        // for JPA
    }

    private Chat(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public static Chat create(User user, String title) {
        return new Chat(user, title);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

