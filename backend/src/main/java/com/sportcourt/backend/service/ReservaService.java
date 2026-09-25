package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.ReservaDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Reserva;
import com.sportcourt.backend.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Servicio de negocio para reservas
 * CRÍTICO: Implementa validaciones complejas de duplicados y capacidad
 */
@Service
@Transactional
public class ReservaService {

    /** Estado con el que el backend crea toda reserva nueva. */
    public static final String ESTADO_INICIAL = "confirmada";

    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;
    private final CanchaService canchaService;

    public ReservaService(ReservaRepository reservaRepository,
            UsuarioService usuarioService,
            CanchaService canchaService) {
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
        this.canchaService = canchaService;
    }

    /**
     * Crear una nueva reserva con validaciones de negocio
     * 
     * Validaciones:
     * 1. Usuario existe
     * 2. Cancha existe
     * 3. Hora inicio < Hora fin
     * 4. NO hay reserva duplicada (mismo usuario, cancha, fecha, horas
     * superpuestas)
     * 5. Capacidad disponible
     */
    public Reserva crearReserva(ReservaDTO reservaDTO) {

        // Usuario autenticado: no confiar en el usuarioId enviado por el cliente
        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();
        reservaDTO.setUsuarioId(usuarioAutenticadoId);

        // Validación 2: Cancha existe
        canchaService.obtenerCancha(reservaDTO.getCanchaId());

        // Serializa las reservas concurrentes sobre la misma cancha hasta
        // que esta transacción termine (evita superar la capacidad).
        canchaService.bloquearCancha(reservaDTO.getCanchaId());

        // Validación 3: Hora válida
        validarHorarios(reservaDTO.getHoraInicio(), reservaDTO.getHoraFin());

        // Validación 4: NO hay duplicados
        validarNoDuplicada(reservaDTO, null);

        // Validación 5: Capacidad disponible
        validarCapacidadDisponible(
                reservaDTO.getCanchaId(),
                reservaDTO.getFecha(),
                reservaDTO.getHoraInicio(),
                reservaDTO.getHoraFin(),
                null);

        // Crear y guardar
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(reservaDTO.getUsuarioId());
        reserva.setCanchaId(reservaDTO.getCanchaId());
        reserva.setFecha(reservaDTO.getFecha());
        reserva.setHoraInicio(reservaDTO.getHoraInicio());
        reserva.setHoraFin(reservaDTO.getHoraFin());
        // El estado lo decide el backend, no el cliente.
        reserva.setEstado(ESTADO_INICIAL);

        return reservaRepository.save(reserva);
    }

    /**
     * Actualizar una reserva existente (su dueño o un ADMIN).
     * Cambia cancha, fecha y horario; el dueño y el estado se conservan
     * (para cancelar se usa cancelarReserva).
     */
    public Reserva actualizarReserva(Integer id, ReservaDTO reservaDTO) {
        Reserva reserva = obtenerReserva(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        if (!esAdmin && !reserva.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para modificar esta reserva");
        }

        // Validaciones similares a crear (excepto duplicados con ella misma)
        canchaService.obtenerCancha(reservaDTO.getCanchaId());
        canchaService.bloquearCancha(reservaDTO.getCanchaId());
        validarHorarios(reservaDTO.getHoraInicio(), reservaDTO.getHoraFin());

        // Los duplicados se validan contra el dueño de la reserva, que no
        // cambia aunque la edite un ADMIN.
        reservaDTO.setUsuarioId(reserva.getUsuarioId());

        // Validar que no exista otra reserva en ese horario
        validarNoDuplicada(reservaDTO, id);

        validarCapacidadDisponible(
                reservaDTO.getCanchaId(),
                reservaDTO.getFecha(),
                reservaDTO.getHoraInicio(),
                reservaDTO.getHoraFin(),
                id);

        reserva.setCanchaId(reservaDTO.getCanchaId());
        reserva.setFecha(reservaDTO.getFecha());
        reserva.setHoraInicio(reservaDTO.getHoraInicio());
        reserva.setHoraFin(reservaDTO.getHoraFin());

        return reservaRepository.save(reserva);
    }

    /**
     * Obtener reserva por ID
     */
    public Reserva obtenerReserva(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva con ID " + id + " no encontrada"));
    }

    /**
     * Listar todas las reservas
     */
    public List<Reserva> listarReservas() {
    Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    boolean esAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    if (esAdmin) {
        return reservaRepository.findAll();
    }

    Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

    return reservaRepository.findByUsuarioId(usuarioAutenticadoId);
}

    /**
     * Cancelar una reserva
     */
    public Reserva cancelarReserva(Integer id) {
        Reserva reserva = obtenerReserva(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        if (!esAdmin && !reserva.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para cancelar esta reserva");
        }

        reserva.setEstado("cancelada");
        return reservaRepository.save(reserva);
    }

    /**
     * Eliminar una reserva
     */
    public void eliminarReserva(Integer id) {
        Reserva reserva = obtenerReserva(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        if (!esAdmin && !reserva.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para eliminar esta reserva");
        }

        reservaRepository.deleteById(id);
    }

    /**
     * VALIDACIÓN 3: Verificar que horaInicio < horaFin
     */
    private void validarHorarios(java.time.LocalTime horaInicio, java.time.LocalTime horaFin) {
        if (horaInicio == null || horaFin == null) {
            throw new BusinessException("Horas no pueden ser nulas");
        }

        if (horaInicio.isAfter(horaFin) || horaInicio.equals(horaFin)) {
            throw new BusinessException("Hora inicio debe ser menor a hora fin");
        }
    }

    /**
     * VALIDACIÓN 4: Verificar que el usuario no tenga
     * otra reserva superpuesta en la misma cancha y fecha.
     */
    private void validarNoDuplicada(ReservaDTO reservaDTO, Integer reservaIdExcluir) {

        List<Reserva> reservasDuplicadas = reservaRepository.buscarReservasDuplicadas(
                reservaDTO.getUsuarioId(),
                reservaDTO.getCanchaId(),
                reservaDTO.getFecha(),
                reservaDTO.getHoraInicio(),
                reservaDTO.getHoraFin(),
                reservaIdExcluir);

        if (!reservasDuplicadas.isEmpty()) {
            Reserva reservaExistente = reservasDuplicadas.get(0);

            throw new BusinessException(
                    "El usuario ya tiene una reserva en ese horario. " +
                            "Cancha: " + reservaDTO.getCanchaId() +
                            ", Fecha: " + reservaDTO.getFecha() +
                            ", Horas: " + reservaExistente.getHoraInicio() +
                            "-" +
                            reservaExistente.getHoraFin());
        }
    }

    /**
     * VALIDACIÓN 5: Verificar capacidad disponible
     * 
     * Capacidad disponible = capacidad total - reservas activas en misma cancha y
     * fecha
     */
    private void validarCapacidadDisponible(
            Integer canchaId,
            java.time.LocalDate fecha,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin,
            Integer reservaIdExcluir) {

        // Canchas sin capacidad definida en la BD (dato legado anterior a la
        // columna "capacidad") se tratan como uso exclusivo: 1 reserva activa
        // a la vez, en vez de lanzar NullPointerException al desempaquetar.
        Integer capacidadTotal = canchaService.obtenerCapacidadCancha(canchaId);
        if (capacidadTotal == null) {
            capacidadTotal = 1;
        }

        long reservasActivas = reservaRepository.buscarReservasSuperpuestas(
                canchaId,
                fecha,
                horaInicio,
                horaFin,
                reservaIdExcluir).stream()
                .filter(r -> !"cancelada".equalsIgnoreCase(r.getEstado()))
                .count();

        if (reservasActivas >= capacidadTotal) {
            throw new BusinessException(
                    "No hay capacidad disponible en la cancha. " +
                            "Capacidad: " + capacidadTotal + ", " +
                            "Reservas activas: " + reservasActivas);
        }

    }

    /**
     * Obtiene el ID del usuario autenticado a partir de la sesión.
     * No confía en el usuarioId enviado por el cliente.
     */
    public Integer obtenerUsuarioAutenticadoId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Usuario no autenticado");
        }

        String email = authentication.getName();

        return usuarioService.obtenerUsuarioPorEmail(email).getId();
    }

}