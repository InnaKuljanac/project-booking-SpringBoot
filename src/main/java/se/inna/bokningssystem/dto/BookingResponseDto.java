package se.inna.bokningssystem.dto;

import java.time.LocalDateTime;

public class BookingResponseDto {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Long userId;
    private Long treatmentId;

    public BookingResponseDto() {
    }

    public BookingResponseDto(
            Long id,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            Long userId,
            Long treatmentId) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.userId = userId;
        this.treatmentId = treatmentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
    }
}