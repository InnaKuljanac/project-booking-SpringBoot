
package se.inna.bokningssystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.inna.bokningssystem.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByTreatmentId(Long treatmentId);

    boolean existsByTreatmentIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long treatmentId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    Optional<Booking> findByIdAndUserUsername(Long id, String username);
}
