package se.inna.bokningssystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "treatments")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int duration;

    @Column(nullable = false)
    private double price;

    @Column(nullable = true)
    private boolean active = true;

    protected Treatment() {
    }

    public Treatment(
            String name,
            String description,
            int duration,
            double price
            ) {

        this.name = name;
        this.description = description;
        this.duration = duration;
        this.price = price;

    }

    public Long getId() {
        return id;
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

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}