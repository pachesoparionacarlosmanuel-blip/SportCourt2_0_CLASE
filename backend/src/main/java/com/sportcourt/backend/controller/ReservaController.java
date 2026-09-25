package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.ReservaDTO;
import com.sportcourt.backend.model.Reserva;
import com.sportcourt.backend.service.ReservaService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestionar reservas
 * Utiliza ReservaService para lógica de negocio con validaciones complejas
 */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Reserva de canchas y consulta/cancelación de reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Obtener todas las reservas
     */
    @GetMapping
    public ResponseEntity<List<Reserva>> listarReservas() {
        List<Reserva> reservas = reservaService.listarReservas();
        return ResponseEntity.ok(reservas);
    }

    /**
     * Obtener una reserva por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarReserva(
            @PathVariable Integer id,
            org.springframework.security.core.Authentication authentication) {

        Reserva reserva = reservaService.obtenerReserva(id);

        Integer usuarioAutenticadoId = reservaService.obtenerUsuarioAutenticadoId();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin && !reserva.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para consultar esta reserva");
        }

        return ResponseEntity.ok(reserva);
    }

    /**
     * Crear una nueva reserva
     * Validaciones: usuario existe, cancha existe, horarios válidos, no duplicados,
     * capacidad disponible
     */
    @PostMapping
    public ResponseEntity<Reserva> crearReserva(@Valid @RequestBody ReservaDTO reservaDTO) {
        Reserva reservaSaved = reservaService.crearReserva(reservaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaSaved);
    }

    /**
     * Actualizar una reserva existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> actualizarReserva(
            @PathVariable Integer id,
            @Valid @RequestBody ReservaDTO reservaDTO) {

        Reserva reservaSaved = reservaService.actualizarReserva(id, reservaDTO);
        return ResponseEntity.ok(reservaSaved);
    }

    /**
     * Cancelar una reserva
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelarReserva(@PathVariable Integer id) {
        Reserva reservaSaved = reservaService.cancelarReserva(id);
        return ResponseEntity.ok(reservaSaved);
    }

    /**
     * Eliminar una reserva
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReserva(@PathVariable Integer id) {
        reservaService.eliminarReserva(id);
        return ResponseEntity.noContent().build();
    }
}