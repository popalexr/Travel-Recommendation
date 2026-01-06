package eu.popalexr.travel_recommendation.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "trip_profiles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_profiles_chat", columnNames = "chat_id")
    }
)
public class TripProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false, unique = true)
    private Chat chat;

    @Column(length = 180)
    private String destination;

    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "end_date", length = 20)
    private String endDate;

    @Column(length = 120)
    private String budget;

    @Column(length = 120)
    private String travelers;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String interests;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String constraints;

    protected TripProfile() {
        // for JPA
    }

    private TripProfile(Chat chat) {
        this.chat = chat;
    }

    public static TripProfile create(Chat chat) {
        return new TripProfile(chat);
    }

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = normalize(destination);
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = normalize(startDate);
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = normalize(endDate);
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = normalize(budget);
    }

    public String getTravelers() {
        return travelers;
    }

    public void setTravelers(String travelers) {
        this.travelers = normalize(travelers);
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = normalize(interests);
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = normalize(constraints);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
