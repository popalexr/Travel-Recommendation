package eu.popalexr.travel_recommendation.Controllers;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import eu.popalexr.travel_recommendation.Models.User;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class SettingsApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsApiController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static class ProfileUpdateRequest {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class PasswordUpdateRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    @PostMapping("/api/settings/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
        @RequestBody ProfileUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Profile data is required."
            ));
        }

        Optional<User> userOpt = getAuthenticatedUser(httpRequest);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Authentication required."
            ));
        }

        String firstName = normalizeValue(request.getFirstName());
        String lastName = normalizeValue(request.getLastName());
        if (exceedsLength(firstName, 80) || exceedsLength(lastName, 80)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Name fields must be at most 80 characters."
            ));
        }

        User user = userOpt.get();
        user.updateProfile(firstName, lastName);
        userRepository.save(user);

        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());

        return ResponseEntity.ok(Map.of(
            "profile", profile
        ));
    }

    @PostMapping("/api/settings/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
        @RequestBody PasswordUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        if (request == null || isBlank(request.getCurrentPassword()) || isBlank(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Current and new passwords are required."
            ));
        }

        Optional<User> userOpt = getAuthenticatedUser(httpRequest);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Authentication required."
            ));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Current password is incorrect."
            ));
        }

        String newPassword = request.getNewPassword().trim();
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "New password must be at least 8 characters."
            ));
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Password updated."
        ));
    }

    private Optional<User> getAuthenticatedUser(HttpServletRequest request) {
        Object uid = request.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (!(uid instanceof Long userId)) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean exceedsLength(String value, int max) {
        return value != null && value.length() > max;
    }
}
