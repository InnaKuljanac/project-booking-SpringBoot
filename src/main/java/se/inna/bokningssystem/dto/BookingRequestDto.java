package se.inna.bokningssystem.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class BookingRequestDto {

    @NotNull(message = "Starttid får inte vara tom")
    @Future(message = "Starttiden måste vara i framtiden")
    private LocalDateTime startTime;

    @NotNull(message = "TreatmentId får inte vara tomt")
    private Long treatmentId;

    public BookingRequestDto() {
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
    }
}