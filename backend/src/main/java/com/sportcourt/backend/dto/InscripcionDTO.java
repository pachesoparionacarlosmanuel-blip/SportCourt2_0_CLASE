package com.sportcourt.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO para Inscripción con validaciones
 */
public class InscripcionDTO {

    private Integer id;

    private Integer usuarioId;

    @NotNull(message = "El ID de la clase es requerido")
    @Positive(message = "El ID de la clase debe ser un número positivo")
    private Integer claseId;

    @FutureOrPresent(message = "La fecha no puede ser en el pasado")
    private LocalDate fecha;

    // Lo asigna el backend (ver servicio); cualquier valor enviado se ignora.
    private String estado;

    // Constructores
    public InscripcionDTO() {
    }

    public InscripcionDTO(Integer id, Integer usuarioId, Integer claseId, LocalDate fecha, String estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.claseId = claseId;
        this.fecha = fecha;
        this.estado = estado;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getClaseId() {
        return claseId;
    }

    public void setClaseId(Integer claseId) {
        this.claseId = claseId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
