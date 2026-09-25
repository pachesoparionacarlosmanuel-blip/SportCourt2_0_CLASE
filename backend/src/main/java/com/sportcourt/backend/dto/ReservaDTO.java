package com.sportcourt.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para Reserva con validaciones
 */
public class ReservaDTO {

    private Integer id;

    private Integer usuarioId;

    @NotNull(message = "El ID de la cancha es requerido")
    @Positive(message = "El ID de la cancha debe ser un número positivo")
    private Integer canchaId;

    @NotNull(message = "La fecha es requerida")
    @FutureOrPresent(message = "La fecha no puede ser en el pasado")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime horaFin;

    // Lo asigna el backend (ver servicio); cualquier valor enviado se ignora.
    private String estado;

    // Constructores
    public ReservaDTO() {
    }

    public ReservaDTO(Integer id, Integer usuarioId, Integer canchaId, LocalDate fecha,
                      LocalTime horaInicio, LocalTime horaFin, String estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.canchaId = canchaId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
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

    public Integer getCanchaId() {
        return canchaId;
    }

    public void setCanchaId(Integer canchaId) {
        this.canchaId = canchaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
