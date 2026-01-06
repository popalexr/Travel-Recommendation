package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Models.Chat;
import eu.popalexr.travel_recommendation.Models.TripProfile;
import eu.popalexr.travel_recommendation.Repositories.ChatRepository;
import eu.popalexr.travel_recommendation.Repositories.TripProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TripProfileController {

    private final TripProfileRepository tripProfileRepository;
    private final ChatRepository chatRepository;

    public TripProfileController(TripProfileRepository tripProfileRepository, ChatRepository chatRepository) {
        this.tripProfileRepository = tripProfileRepository;
        this.chatRepository = chatRepository;
    }

    public static class TripProfileRequest {
        private String destination;
        private String startDate;
        private String endDate;
        private String budget;
        private String travelers;
        private String interests;
        private String constraints;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getBudget() {
            return budget;
        }

        public void setBudget(String budget) {
            this.budget = budget;
        }

        public String getTravelers() {
            return travelers;
        }

        public void setTravelers(String travelers) {
            this.travelers = travelers;
        }

        public String getInterests() {
            return interests;
        }

        public void setInterests(String interests) {
            this.interests = interests;
        }

        public String getConstraints() {
            return constraints;
        }

        public void setConstraints(String constraints) {
            this.constraints = constraints;
        }
    }

    @GetMapping("/api/chat/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
        @PathVariable("id") Long chatId,
        HttpServletRequest request
    ) {
        Object uid = request.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        Chat chat = chatRepository.findByIdAndUserId(chatId, userId).orElse(null);
        if (chat == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Chat not found.")
            );
        }

        TripProfile profile = tripProfileRepository.findByChatId(chatId).orElse(null);
        return ResponseEntity.ok(Map.of("profile", profileDto(profile)));
    }

    @PostMapping("/api/chat/{id}/profile")
    public ResponseEntity<Map<String, Object>> saveProfile(
        @PathVariable("id") Long chatId,
        @RequestBody TripProfileRequest request,
        HttpServletRequest httpRequest
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Profile data is required.")
            );
        }

        Object uid = httpRequest.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Authentication required.")
            );
        }

        Chat chat = chatRepository.findByIdAndUserId(chatId, userId).orElse(null);
        if (chat == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Chat not found.")
            );
        }

        TripProfile profile = tripProfileRepository.findByChatId(chatId)
            .orElseGet(() -> TripProfile.create(chat));

        profile.setDestination(request.getDestination());
        profile.setStartDate(request.getStartDate());
        profile.setEndDate(request.getEndDate());
        profile.setBudget(request.getBudget());
        profile.setTravelers(request.getTravelers());
        profile.setInterests(request.getInterests());
        profile.setConstraints(request.getConstraints());

        TripProfile saved = tripProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("profile", profileDto(saved)));
    }

    private Map<String, Object> profileDto(TripProfile profile) {
        if (profile == null) {
            return Map.of(
                "destination", "",
                "startDate", "",
                "endDate", "",
                "budget", "",
                "travelers", "",
                "interests", "",
                "constraints", ""
            );
        }
        return Map.of(
            "destination", nullToEmpty(profile.getDestination()),
            "startDate", nullToEmpty(profile.getStartDate()),
            "endDate", nullToEmpty(profile.getEndDate()),
            "budget", nullToEmpty(profile.getBudget()),
            "travelers", nullToEmpty(profile.getTravelers()),
            "interests", nullToEmpty(profile.getInterests()),
            "constraints", nullToEmpty(profile.getConstraints())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
