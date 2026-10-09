package se.inna.bokningssystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.inna.bokningssystem.model.Treatment;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    boolean existsByNameIgnoreCase(String name);
}