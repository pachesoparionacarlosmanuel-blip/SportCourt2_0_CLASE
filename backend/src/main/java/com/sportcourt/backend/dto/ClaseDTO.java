package com.sportcourt.backend.dto;

import jakarta.validation.constraints.*;

/**
 * DTO para Clase con validaciones
 */
public class ClaseDTO {

    private Integer id;

    @NotBlank(message = "El nombre de la clase es requerido")
    private String name;

    @NotBlank(message = "El icono es requerido")
    private String icon;

    @NotBlank(message = "El nivel es requerido")
    private String level;

    @NotBlank(message = "El horario es requerido")
    private String schedule;

    @NotBlank(message = "El profesor es requerido")
    private String professor;

    @Positive(message = "El precio debe ser mayor a 0")
    private double price;

    @Positive(message = "Los cupos disponibles deben ser mayor a 0")
    private int slots;

    // Constructores
    public ClaseDTO() {
    }

    public ClaseDTO(Integer id, String name, String icon, String level, String schedule,
                    String professor, double price, int slots) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.level = level;
        this.schedule = schedule;
        this.professor = professor;
        this.price = price;
        this.slots = slots;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getProfessor() {
        return professor;
    }

    public void setProfessor(String professor) {
        this.professor = professor;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }
}
