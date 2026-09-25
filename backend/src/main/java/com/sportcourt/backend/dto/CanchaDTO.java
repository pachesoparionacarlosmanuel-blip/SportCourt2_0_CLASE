package com.sportcourt.backend.dto;

import jakarta.validation.constraints.*;

/**
 * DTO para Cancha con validaciones
 */
public class CanchaDTO {

    private Integer id;

    @NotBlank(message = "El tipo de deporte es requerido")
    private String sport;

    @NotBlank(message = "El nombre de la cancha es requerido")
    private String name;

    @NotBlank(message = "La ubicación es requerida")
    private String location;

    @Positive(message = "El precio debe ser mayor a 0")
    private double price;

    @NotBlank(message = "El estado es requerido")
    private String status;

    @NotBlank(message = "La descripción es requerida")
    private String description;

    @Positive(message = "La capacidad debe ser mayor a 0")
    private Integer capacity;

    private String image;

    // Constructores
    public CanchaDTO() {
    }

    public CanchaDTO(Integer id, String sport, String name, String location, double price,
                     String status, String description, Integer capacity, String image) {
        this.id = id;
        this.sport = sport;
        this.name = name;
        this.location = location;
        this.price = price;
        this.status = status;
        this.description = description;
        this.capacity = capacity;
        this.image = image;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
