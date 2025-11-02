package eu.popalexr.travel_recommendation.Services.Impl;

import eu.popalexr.travel_recommendation.DTOs.LoginRequest;
import eu.popalexr.travel_recommendation.DTOs.RegisterRequest;
import eu.popalexr.travel_recommendation.Exceptions.EmailAlreadyInUseException;
import eu.popalexr.travel_recommendation.Exceptions.InvalidCredentialsException;
import eu.popalexr.travel_recommendation.Models.User;
import eu.popalexr.travel_recommendation.Repositories.UserRepository;
import eu.popalexr.travel_recommendation.Services.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        final String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyInUseException("Email is already registered.");
        }

        User user = User.create(
            normalizedEmail,
            passwordEncoder.encode(request.getPassword()),
            request.getFirstName(),
            request.getLastName()
        );

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        return userRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
            .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));
    }
}
