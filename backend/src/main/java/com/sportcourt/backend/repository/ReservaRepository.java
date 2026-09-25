package com.sportcourt.backend.repository;

import com.sportcourt.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /**
     * Busca reservas activas de una cancha en una fecha
     * cuyos horarios se superponen.
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.canchaId = :canchaId
        AND r.fecha = :fecha
        AND r.estado <> 'cancelada'
        AND (:reservaId IS NULL OR r.id <> :reservaId)
        AND r.horaInicio < :horaFin
        AND r.horaFin > :horaInicio
        """)
    List<Reserva> buscarReservasSuperpuestas(
            @Param("canchaId") Integer canchaId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("reservaId") Integer reservaId);

    /**
     * Busca reservas del mismo usuario, cancha y fecha
     * cuyos horarios se superponen.
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.usuarioId = :usuarioId
        AND r.canchaId = :canchaId
        AND r.fecha = :fecha
        AND r.estado <> 'cancelada'
        AND (:reservaId IS NULL OR r.id <> :reservaId)
        AND r.horaInicio < :horaFin
        AND r.horaFin > :horaInicio
        """)
    List<Reserva> buscarReservasDuplicadas(
            @Param("usuarioId") Integer usuarioId,
            @Param("canchaId") Integer canchaId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("reservaId") Integer reservaId);

    /**
     * Obtiene las reservas de un usuario.
     */
    List<Reserva> findByUsuarioId(Integer usuarioId);
}