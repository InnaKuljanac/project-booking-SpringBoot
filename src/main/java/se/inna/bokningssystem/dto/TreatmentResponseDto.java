package se.inna.bokningssystem.dto;

public class TreatmentResponseDto {

    private Long id;
    private String name;
    private String description;
    private int duration;
    private double price;
    private boolean active;



    public TreatmentResponseDto(
            Long id,
            String name,
            String description,
            int duration,
            double price,
            boolean active) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.price = price;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public double getPrice() {
        return price;
    }


    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


}