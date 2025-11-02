package eu.popalexr.travel_recommendation.Services;

import eu.popalexr.travel_recommendation.DTOs.LoginRequest;
import eu.popalexr.travel_recommendation.DTOs.RegisterRequest;
import eu.popalexr.travel_recommendation.Models.User;

public interface UserService {
    User register(RegisterRequest request);
    User authenticate(LoginRequest request);
}
