package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.InscripcionDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Inscripcion;
import com.sportcourt.backend.repository.InscripcionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Servicio de negocio para inscripciones
 * CRÍTICO: Implementa validaciones de duplicados y cupos disponibles
 */
@Service
@Transactional
public class InscripcionService {

    /** Estado con el que el backend crea toda inscripción nueva. */
    public static final String ESTADO_INICIAL = "inscrita";

    private final InscripcionRepository inscripcionRepository;
    private final UsuarioService usuarioService;
    private final ClaseService claseService;

    public InscripcionService(InscripcionRepository inscripcionRepository,
            UsuarioService usuarioService,
            ClaseService claseService) {
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioService = usuarioService;
        this.claseService = claseService;
    }

    /**
     * Crear una nueva inscripción con validaciones de negocio
     * 
     * Validaciones:
     * 1. Usuario existe
     * 2. Clase existe
     * 3. NO hay inscripción duplicada (mismo usuario, clase)
     * 4. Hay cupos disponibles
     */
    public Inscripcion crearInscripcion(InscripcionDTO inscripcionDTO) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin) {
            throw new BusinessException("Los administradores no pueden inscribirse a clases");
        }

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();
        inscripcionDTO.setUsuarioId(usuarioAutenticadoId);

        // Usuario autenticado: no confiar en el usuarioId enviado por el cliente
        // El ID ya fue obtenido desde la sesión.

        // Validación 2: Clase existe
        claseService.obtenerClase(inscripcionDTO.getClaseId());

        // Serializa las inscripciones concurrentes a la misma clase hasta
        // que esta transacción termine (evita superar los cupos).
        claseService.bloquearClase(inscripcionDTO.getClaseId());

        // Validación 3: NO hay inscripción duplicada
        validarNoDuplicada(inscripcionDTO);

        // Validación 4: Hay cupos disponibles
        validarCuposDisponibles(inscripcionDTO.getClaseId());

        // Crear y guardar
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setUsuarioId(inscripcionDTO.getUsuarioId());
        inscripcion.setClaseId(inscripcionDTO.getClaseId());
        inscripcion.setFecha(inscripcionDTO.getFecha());
        // El estado lo decide el backend, no el cliente.
        inscripcion.setEstado(ESTADO_INICIAL);

        return inscripcionRepository.save(inscripcion);
    }

    /**
     * Obtener inscripción por ID
     */
    public Inscripcion obtenerInscripcion(Integer id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción con ID " + id + " no encontrada"));
    }

    /**
     * Listar todas las inscripciones
     */
    public List<Inscripcion> listarInscripciones() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin) {
            return inscripcionRepository.findAll();
        }

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        return inscripcionRepository.findByUsuarioId(usuarioAutenticadoId);
    }

    /**
     * Obtener inscripciones de un usuario
     */
    public List<Inscripcion> obtenerInscripcionesDeUsuario(Integer usuarioId) {
        usuarioService.verificarUsuarioExiste(usuarioId);
        return inscripcionRepository.findByUsuarioId(usuarioId);
    }

    /**
     * Cancelar una inscripción
     */
    public Inscripcion cancelarInscripcion(Integer id) {
        Inscripcion inscripcion = obtenerInscripcion(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        if (!esAdmin && !inscripcion.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para cancelar esta inscripción");
        }

        inscripcion.setEstado("cancelada");

        return inscripcionRepository.save(inscripcion);
    }

    /**
     * Eliminar una inscripción
     */
    public void eliminarInscripcion(Integer id) {
        Inscripcion inscripcion = obtenerInscripcion(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Integer usuarioAutenticadoId = obtenerUsuarioAutenticadoId();

        if (!esAdmin && !inscripcion.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para eliminar esta inscripción");
        }

        inscripcionRepository.deleteById(id);
    }

    /**
     * VALIDACIÓN 3: Verificar NO hay inscripción duplicada
     * 
     * Criterios de duplicado:
     * - Mismo usuario
     * - Misma clase
     * - Estado NO cancelada
     */
    private void validarNoDuplicada(InscripcionDTO inscripcionDTO) {
        boolean yaInscrito = inscripcionRepository.existeInscripcionActiva(
                inscripcionDTO.getUsuarioId(),
                inscripcionDTO.getClaseId());

        if (yaInscrito) {
            throw new BusinessException(
                    "El usuario ya está inscrito en esta clase. " +
                            "Clase ID: " + inscripcionDTO.getClaseId());
        }
    }

    /**
     * VALIDACIÓN 4: Verificar cupos disponibles
     * 
     * Cupos disponibles = cupos totales - inscripciones activas
     */
    private void validarCuposDisponibles(Integer claseId) {
        // Una clase sin cupos definidos no admite inscripciones (antes lanzaba
        // NullPointerException al desempaquetar y respondía 500).
        Integer cuposTotales = claseService.obtenerCuposDisponibles(claseId);
        if (cuposTotales == null) {
            cuposTotales = 0;
        }

        // Contar inscripciones activas en esta clase
        long inscripcionesActivas = inscripcionRepository.contarInscripcionesActivas(claseId);

        if (inscripcionesActivas >= cuposTotales) {
            throw new BusinessException(
                    "No hay cupos disponibles en la clase. " +
                            "Cupos: " + cuposTotales + ", " +
                            "Inscripciones activas: " + inscripcionesActivas);
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
