package se.inna.bokningssystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TreatmentRequestDto {

    @NotBlank(message = "Namn får inte vara tomt")
    private String name;

    @NotBlank(message = "Beskrivning får inte vara tom")
    private String description;

    @NotNull(message = "Pris får inte vara tomt")
    @Positive(message = "Pris måste vara större än 0")
    private Double price;


    @Positive(message = "Behandlingstid måste vara längre än 0")
    private int duration;




    public TreatmentRequestDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }



}