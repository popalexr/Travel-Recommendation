package eu.popalexr.travel_recommendation.Repositories;

import eu.popalexr.travel_recommendation.Models.TripProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripProfileRepository extends JpaRepository<TripProfile, Long> {

    Optional<TripProfile> findByChatId(Long chatId);

    void deleteByChatId(Long chatId);
}
