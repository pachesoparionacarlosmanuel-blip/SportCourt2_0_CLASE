package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.InscripcionDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.model.Inscripcion;
import com.sportcourt.backend.repository.InscripcionRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sportcourt.backend.model.Usuario;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para InscripcionService
 * 
 * Casos críticos:
 * 1. Usuario existe
 * 2. Clase existe
 * 3. NO hay inscripción duplicada (mismo usuario + clase)
 * 4. Hay cupos disponibles
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InscripcionService - Tests Unitarios")
public class InscripcionServiceTest {
    private void autenticarUsuario(Integer usuarioId, String email, String rol) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + rol)));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(usuarioId);
        usuarioMock.setEmail(email);

        when(usuarioService.obtenerUsuarioPorEmail(email))
                .thenReturn(usuarioMock);
    }

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ClaseService claseService;

    @InjectMocks
    private InscripcionService inscripcionService;

    private InscripcionDTO validInscripcionDTO;
    private Inscripcion mockInscripcion;

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        // DTO válido para usar en tests
        validInscripcionDTO = new InscripcionDTO();
        validInscripcionDTO.setUsuarioId(1);
        validInscripcionDTO.setClaseId(1);
        validInscripcionDTO.setFecha(LocalDate.of(2026, 9, 10));
        validInscripcionDTO.setEstado("activa");

        // Inscripción mock para retornar del save
        mockInscripcion = new Inscripcion();
        mockInscripcion.setId(1);
        mockInscripcion.setUsuarioId(1);
        mockInscripcion.setClaseId(1);
        mockInscripcion.setFecha(LocalDate.of(2026, 9, 10));
        mockInscripcion.setEstado(InscripcionService.ESTADO_INICIAL);

    }

    // ==================== TESTS EXITOSOS ====================

    @Test
    @DisplayName("✅ Crear inscripción exitosa con todos los datos válidos")
    void crearInscripcionExitosa() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange
        when(claseService.obtenerClase(1)).thenReturn(createMockClase());
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(10); // 10 cupos disponibles
        when(inscripcionRepository.existeInscripcionActiva(1, 1)).thenReturn(false);
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(0L); // No hay inscripciones existentes
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals(1, resultado.getUsuarioId());
        assertEquals(1, resultado.getClaseId());
        assertEquals(InscripcionService.ESTADO_INICIAL, resultado.getEstado());
        verify(claseService).obtenerClase(1);
        verify(claseService).bloquearClase(1);
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("✅ Obtener inscripción existente por ID")
    void obtenerInscripcionExistente() {
        // Arrange
        when(inscripcionRepository.findById(1)).thenReturn(Optional.of(mockInscripcion));

        // Act
        Inscripcion resultado = inscripcionService.obtenerInscripcion(1);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        verify(inscripcionRepository).findById(1);
    }

    @Test
    @DisplayName("✅ Cancelar inscripción exitosamente")
    void cancelarInscripcionExitosa() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange
        when(inscripcionRepository.findById(1)).thenReturn(Optional.of(mockInscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.cancelarInscripcion(1);

        // Assert
        assertEquals("cancelada", resultado.getEstado());
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("✅ Listar todas las inscripciones")
    void listarInscripciones() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange
        List<Inscripcion> inscripciones = List.of(mockInscripcion);
        when(inscripcionRepository.findByUsuarioId(1)).thenReturn(inscripciones);

        // Act
        List<Inscripcion> resultado = inscripcionService.listarInscripciones();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(inscripcionRepository).findByUsuarioId(1);
        verify(inscripcionRepository, never()).findAll();
    }

    @Test
    @DisplayName("🔒 SEGURIDAD: Un administrador no puede inscribirse a una clase")
    void crearInscripcionRechazadaParaAdmin() {

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin@test.com",
                null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            inscripcionService.crearInscripcion(validInscripcionDTO);
        });

        verify(inscripcionRepository, never()).save(any());
    }

    // ==================== TESTS DE ERROR - VALIDACIÓN 1 ====================

    @Test
    @DisplayName("🔒 SEGURIDAD: No permite elegir otro usuario mediante usuarioId")
    void crearInscripcionUsuarioNoExiste() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        when(claseService.obtenerCuposDisponibles(1)).thenReturn(10);

        when(inscripcionRepository.save(any(Inscripcion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        InscripcionDTO dto = new InscripcionDTO();
        dto.setUsuarioId(999); // Intento de suplantar a otro usuario
        dto.setClaseId(1);
        dto.setFecha(LocalDate.of(2026, 9, 10));
        dto.setEstado("activa");

        Inscripcion resultado = inscripcionService.crearInscripcion(dto);

        // El usuarioId enviado por el cliente debe ser ignorado
        assertEquals(1, resultado.getUsuarioId());

        // Se debe guardar la inscripción asociada al usuario autenticado
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    // ==================== TESTS DE ERROR - VALIDACIÓN 2 ====================

    @Test
    @DisplayName("❌ VALIDACIÓN 2: Clase NO existe")
    void crearInscripcionClaseNoExiste() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange
        when(claseService.obtenerClase(999))
                .thenThrow(new ResourceNotFoundException("Clase con ID 999 no encontrada"));

        InscripcionDTO dtoConClaseInvalida = new InscripcionDTO();
        dtoConClaseInvalida.setUsuarioId(1);
        dtoConClaseInvalida.setClaseId(999);
        dtoConClaseInvalida.setFecha(LocalDate.of(2026, 9, 10));
        dtoConClaseInvalida.setEstado("activa");

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            inscripcionService.crearInscripcion(dtoConClaseInvalida);
        });

        verify(claseService).obtenerClase(999);
        verify(inscripcionRepository, never()).save(any());
    }

    // ==================== TESTS DE ERROR - VALIDACIÓN 3 ====================

    @Test
    @DisplayName("❌ VALIDACIÓN 3: Inscripción duplicada - usuario ya en clase")
    void crearInscripcionDuplicada() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: Usuario ya está inscrito (inscripción no cancelada)
        when(claseService.obtenerClase(1)).thenReturn(createMockClase());
        when(inscripcionRepository.existeInscripcionActiva(1, 1)).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            inscripcionService.crearInscripcion(validInscripcionDTO);
        });

        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ NO hay duplicada - inscripción anterior CANCELADA")
    void crearInscripcionCancelada() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: la inscripción anterior está CANCELADA, así que la consulta
        // de inscripciones activas no la encuentra ni la cuenta (el filtro por
        // estado se prueba contra H2 en InscripcionControllerTest).
        when(claseService.obtenerClase(1)).thenReturn(createMockClase());
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(10); // 10 cupos disponibles
        when(inscripcionRepository.existeInscripcionActiva(1, 1)).thenReturn(false);
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(0L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        // Assert
        assertNotNull(resultado);
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("✅ NO hay duplicada - es usuario diferente en MISMA clase")
    void crearInscripcionUsuarioDiferente() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: Otro usuario está en la misma clase (ocupa 1 cupo)
        when(claseService.obtenerClase(1)).thenReturn(createMockClase());
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(10); // 10 cupos disponibles
        when(inscripcionRepository.existeInscripcionActiva(1, 1)).thenReturn(false);
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(1L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        // Assert
        assertNotNull(resultado);
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    // ==================== TESTS DE ERROR - VALIDACIÓN 4 ====================

    @Test
    @DisplayName("❌ VALIDACIÓN 4: No hay cupos disponibles")
    void crearInscripcionSinCupos() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: Clase con 1 cupo
        when(claseService.obtenerClase(1)).thenReturn(createMockClase()); // 1 cupo
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(1);

        // Ya hay 1 inscripción activa
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(1L);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            inscripcionService.crearInscripcion(validInscripcionDTO);
        });

        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ VALIDACIÓN 4: Hay cupos disponibles")
    void crearInscripcionConCupos() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: Clase con 2 cupos
        when(claseService.obtenerClase(1)).thenReturn(createMockClase()); // 1 cupo
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(2); // 2 cupos

        // Ya hay 1 inscripción activa
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(1L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        // Assert
        assertNotNull(resultado);
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("✅ VALIDACIÓN 4: Hay cupos disponibles (2/3)")
    void crearInscripcionConCuposMultiples() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        // Arrange: Clase con 3 cupos
        when(claseService.obtenerClase(1)).thenReturn(createMockClase());
        when(claseService.obtenerCuposDisponibles(1)).thenReturn(3);

        // Ya hay 2 inscripciones activas
        when(inscripcionRepository.contarInscripcionesActivas(1)).thenReturn(2L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenReturn(mockInscripcion);

        // Act
        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        // Assert
        assertNotNull(resultado);
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("🔒 El estado lo asigna el backend: se ignora el enviado por el cliente")
    void crearInscripcionIgnoraEstadoDelCliente() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        when(claseService.obtenerCuposDisponibles(1)).thenReturn(10);
        when(inscripcionRepository.save(any(Inscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        validInscripcionDTO.setEstado("cancelada");

        Inscripcion resultado = inscripcionService.crearInscripcion(validInscripcionDTO);

        assertEquals(InscripcionService.ESTADO_INICIAL, resultado.getEstado());
    }

    @Test
    @DisplayName("❌ VALIDACIÓN 4: Clase sin cupos definidos no admite inscripciones (no 500)")
    void crearInscripcionClaseSinCuposDefinidos() {

        autenticarUsuario(1, "usuario@test.com", "USER");

        when(claseService.obtenerCuposDisponibles(1)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> inscripcionService.crearInscripcion(validInscripcionDTO));

        verify(inscripcionRepository, never()).save(any());
    }

    // ==================== HELPER METHODS ====================

    private com.sportcourt.backend.model.Clase createMockClase() {
        com.sportcourt.backend.model.Clase clase = new com.sportcourt.backend.model.Clase();
        clase.setId(1);
        clase.setSlots(1);
        return clase;
    }
}
