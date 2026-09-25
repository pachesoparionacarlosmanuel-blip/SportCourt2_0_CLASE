package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.ReservaDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Reserva;
import com.sportcourt.backend.repository.ReservaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.sportcourt.backend.model.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ReservaService
 * 
 * Casos críticos:
 * 1. Usuario existe
 * 2. Cancha existe
 * 3. Horarios válidos
 * 4. NO hay duplicados (horas superpuestas)
 * 5. Capacidad disponible
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService - Tests Unitarios")
public class ReservaServiceTest {

        @Mock
        private ReservaRepository reservaRepository;

        @Mock
        private UsuarioService usuarioService;

        @Mock
        private CanchaService canchaService;

        @InjectMocks
        private ReservaService reservaService;

        private ReservaDTO validReservaDTO;
        private Reserva mockReserva;

        @BeforeEach
        void setUp() {
                // DTO válido para usar en tests
                validReservaDTO = new ReservaDTO();
                validReservaDTO.setUsuarioId(1);
                validReservaDTO.setCanchaId(1);
                validReservaDTO.setFecha(LocalDate.of(2026, 9, 10));
                validReservaDTO.setHoraInicio(LocalTime.of(10, 0));
                validReservaDTO.setHoraFin(LocalTime.of(11, 0));
                validReservaDTO.setEstado("activa");

                // Reserva mock para retornar del save
                mockReserva = new Reserva();
                mockReserva.setId(1);
                mockReserva.setUsuarioId(1);
                mockReserva.setCanchaId(1);
                mockReserva.setFecha(LocalDate.of(2026, 9, 10));
                mockReserva.setHoraInicio(LocalTime.of(10, 0));
                mockReserva.setHoraFin(LocalTime.of(11, 0));
                mockReserva.setEstado("activa");
        }

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

        @AfterEach
        void limpiarAutenticacion() {
                SecurityContextHolder.clearContext();
        }

        // ==================== TESTS EXITOSOS ====================

        @Test
        @DisplayName("✅ Crear reserva exitosa con todos los datos válidos")
        void crearReservaExitosa() {
                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");
                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(10); // 10 de capacidad
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());
                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                // Act
                Reserva resultado = reservaService.crearReserva(validReservaDTO);

                // Assert
                assertNotNull(resultado);
                assertEquals(1, resultado.getId());
                assertEquals(1, resultado.getUsuarioId());
                assertEquals(1, resultado.getCanchaId());
                assertEquals("activa", resultado.getEstado());
                verify(canchaService).obtenerCancha(1);
                verify(canchaService).bloquearCancha(1);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("🔒 El estado lo asigna el backend: se ignora el enviado por el cliente")
        void crearReservaIgnoraEstadoDelCliente() {
                autenticarUsuario(1, "usuario@test.com", "USER");
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(10);
                when(reservaRepository.save(any(Reserva.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                validReservaDTO.setEstado("cancelada");

                Reserva resultado = reservaService.crearReserva(validReservaDTO);

                assertEquals(ReservaService.ESTADO_INICIAL, resultado.getEstado());
        }

        @Test
        @DisplayName("🔒 Actualizar conserva el estado actual aunque el cliente envíe otro")
        void actualizarReservaConservaEstado() {
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(1);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(8, 0));
                reservaExistente.setHoraFin(LocalTime.of(9, 0));
                reservaExistente.setEstado("confirmada");

                when(reservaRepository.findById(1)).thenReturn(Optional.of(reservaExistente));
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);
                when(reservaRepository.save(any(Reserva.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                validReservaDTO.setEstado("pendiente");

                Reserva resultado = reservaService.actualizarReserva(1, validReservaDTO);

                assertEquals("confirmada", resultado.getEstado());
                assertEquals(LocalTime.of(10, 0), resultado.getHoraInicio());
        }

        @Test
        @DisplayName("Un ADMIN puede actualizar la reserva de otro usuario sin quedarse con ella")
        void adminPuedeActualizarReservaDeOtroUsuario() {
                autenticarUsuario(99, "admin@test.com", "ADMIN");

                // mockReserva pertenece al usuario 1
                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);
                when(reservaRepository.save(any(Reserva.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                validReservaDTO.setHoraInicio(LocalTime.of(12, 0));
                validReservaDTO.setHoraFin(LocalTime.of(13, 0));

                Reserva resultado = reservaService.actualizarReserva(1, validReservaDTO);

                // El dueño se conserva y los duplicados se validan contra él
                assertEquals(1, resultado.getUsuarioId());
                assertEquals(LocalTime.of(12, 0), resultado.getHoraInicio());
                verify(reservaRepository).buscarReservasDuplicadas(
                                eq(1), eq(1), eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(12, 0)), eq(LocalTime.of(13, 0)), eq(1));
        }

        @Test
        @DisplayName("Un usuario no puede actualizar la reserva de otro usuario")
        void usuarioNoPuedeActualizarReservaDeOtroUsuario() {
                autenticarUsuario(2, "otro@test.com", "USER");

                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));

                assertThrows(
                                org.springframework.security.access.AccessDeniedException.class,
                                () -> reservaService.actualizarReserva(1, validReservaDTO));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("✅ Obtener reserva existente por ID")
        void obtenerReservaExistente() {
                // Arrange
                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));

                // Act
                Reserva resultado = reservaService.obtenerReserva(1);

                // Assert
                assertNotNull(resultado);
                assertEquals(1, resultado.getId());
                verify(reservaRepository).findById(1);
        }

        @Test
        @DisplayName("✅ Cancelar reserva exitosamente")
        void cancelarReservaExitosa() {
                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));
                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                // Act
                Reserva resultado = reservaService.cancelarReserva(1);

                // Assert
                assertEquals("cancelada", resultado.getEstado());
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("❌ Usuario no puede cancelar reserva de otro usuario")
        void cancelarReservaDeOtroUsuario() {
                // Arrange
                autenticarUsuario(2, "otro@test.com", "USER");

                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));

                // Act + Assert
                assertThrows(
                                org.springframework.security.access.AccessDeniedException.class,
                                () -> reservaService.cancelarReserva(1));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        // ==================== TESTS DE ERROR - VALIDACIÓN 1 ====================

        @Test
        @DisplayName("❌ VALIDACIÓN 1: Usuario NO existe")
        void crearReservaUsuarioNoExiste() {
                // Arrange
                autenticarUsuario(999, "usuario@test.com", "USER");

                when(usuarioService.obtenerUsuarioPorEmail("usuario@test.com"))
                                .thenThrow(new ResourceNotFoundException("Usuario con ID 999 no encontrado"));

                ReservaDTO dtoConUsuarioInvalido = new ReservaDTO();
                dtoConUsuarioInvalido.setUsuarioId(999);
                dtoConUsuarioInvalido.setCanchaId(1);
                dtoConUsuarioInvalido.setFecha(LocalDate.of(2026, 9, 10));
                dtoConUsuarioInvalido.setHoraInicio(LocalTime.of(10, 0));
                dtoConUsuarioInvalido.setHoraFin(LocalTime.of(11, 0));
                dtoConUsuarioInvalido.setEstado("activa");

                // Act & Assert
                assertThrows(ResourceNotFoundException.class, () -> {
                        reservaService.crearReserva(dtoConUsuarioInvalido);
                });

                verify(reservaRepository, never()).save(any());
        }

        // ==================== TESTS DE ERROR - VALIDACIÓN 2 ====================

        @Test
        @DisplayName("❌ VALIDACIÓN 2: Cancha NO existe")
        void crearReservaCanchaNoExiste() {
                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");
                when(canchaService.obtenerCancha(999))
                                .thenThrow(new ResourceNotFoundException("Cancha con ID 999 no encontrada"));

                ReservaDTO dtoConCanchaInvalida = new ReservaDTO();
                dtoConCanchaInvalida.setUsuarioId(1);
                dtoConCanchaInvalida.setCanchaId(999);
                dtoConCanchaInvalida.setFecha(LocalDate.of(2026, 9, 10));
                dtoConCanchaInvalida.setHoraInicio(LocalTime.of(10, 0));
                dtoConCanchaInvalida.setHoraFin(LocalTime.of(11, 0));
                dtoConCanchaInvalida.setEstado("activa");

                // Act & Assert
                assertThrows(ResourceNotFoundException.class, () -> {
                        reservaService.crearReserva(dtoConCanchaInvalida);
                });

                verify(canchaService).obtenerCancha(999);
                verify(reservaRepository, never()).save(any());
        }

        // ==================== TESTS DE ERROR - VALIDACIÓN 3 ====================

        @Test
        @DisplayName("❌ VALIDACIÓN 3: Hora inicio >= Hora fin (igual)")
        void crearReservaHorasIguales() {
                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                ReservaDTO dtoConHorasIguales = new ReservaDTO();
                dtoConHorasIguales.setUsuarioId(1);
                dtoConHorasIguales.setCanchaId(1);
                dtoConHorasIguales.setFecha(LocalDate.of(2026, 9, 10));
                dtoConHorasIguales.setHoraInicio(LocalTime.of(10, 0));
                dtoConHorasIguales.setHoraFin(LocalTime.of(10, 0)); // IGUAL
                dtoConHorasIguales.setEstado("activa");

                // Act & Assert
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(dtoConHorasIguales);
                });

                verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ VALIDACIÓN 3: Hora inicio > Hora fin")
        void crearReservaHorasInvertidas() {
                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                ReservaDTO dtoConHorasInvertidas = new ReservaDTO();
                dtoConHorasInvertidas.setUsuarioId(1);
                dtoConHorasInvertidas.setCanchaId(1);
                dtoConHorasInvertidas.setFecha(LocalDate.of(2026, 9, 10));
                dtoConHorasInvertidas.setHoraInicio(LocalTime.of(11, 0));
                dtoConHorasInvertidas.setHoraFin(LocalTime.of(10, 0)); // INVERTIDAS
                dtoConHorasInvertidas.setEstado("activa");

                // Act & Assert
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(dtoConHorasInvertidas);
                });

                verify(reservaRepository, never()).save(any());
        }

        // ==================== TESTS DE ERROR - VALIDACIÓN 4 ====================

        @Test
        @DisplayName("❌ VALIDACIÓN 4: Reserva duplicada - horas se superponen COMPLETAMENTE")
        void crearReservaDuplicadaSuperposicionCompleta() {
                // Arrange: Usuario ya tiene reserva 10:00-11:00
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 15)),
                                eq(LocalTime.of(11, 15)),
                                isNull())).thenReturn(List.of(reservaExistente));

                // Intenta hacer reserva 10:15-11:15 (se superpone)
                ReservaDTO dtoDuplicada = new ReservaDTO();
                dtoDuplicada.setUsuarioId(1);
                dtoDuplicada.setCanchaId(1);
                dtoDuplicada.setFecha(LocalDate.of(2026, 9, 10));
                dtoDuplicada.setHoraInicio(LocalTime.of(10, 15));
                dtoDuplicada.setHoraFin(LocalTime.of(11, 15));
                dtoDuplicada.setEstado("activa");

                // Act & Assert
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(dtoDuplicada);
                });

                verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ VALIDACIÓN 4: Reserva duplicada - hora inicio se superpone")
        void crearReservaDuplicadaSuperposicionInicio() {
                // Arrange: Usuario ya tiene reserva 10:00-11:00
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(9, 30)),
                                eq(LocalTime.of(10, 30)),
                                isNull())).thenReturn(List.of(reservaExistente));

                // Intenta hacer reserva 9:30-10:30 (se superpone en inicio)
                ReservaDTO dtoDuplicada = new ReservaDTO();
                dtoDuplicada.setUsuarioId(1);
                dtoDuplicada.setCanchaId(1);
                dtoDuplicada.setFecha(LocalDate.of(2026, 9, 10));
                dtoDuplicada.setHoraInicio(LocalTime.of(9, 30));
                dtoDuplicada.setHoraFin(LocalTime.of(10, 30));
                dtoDuplicada.setEstado("activa");

                // Act & Assert
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(dtoDuplicada);
                });

                verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ VALIDACIÓN 4: Reserva SIN superposición - hora fin exacta = nueva inicio")
        void crearReservaSinSuperposicion() {
                // Arrange: Usuario ya tiene reserva 10:00-11:00
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(10); // 10 de capacidad
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 0)),
                                eq(LocalTime.of(12, 0)),
                                isNull())).thenReturn(List.of());

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 0)),
                                eq(LocalTime.of(12, 0)),
                                isNull())).thenReturn(List.of());
                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                // Intenta hacer reserva 11:00-12:00 (NO se superpone, comienza exactamente
                // cuando termina)
                ReservaDTO dtoValida = new ReservaDTO();
                dtoValida.setUsuarioId(1);
                dtoValida.setCanchaId(1);
                dtoValida.setFecha(LocalDate.of(2026, 9, 10));
                dtoValida.setHoraInicio(LocalTime.of(11, 0));
                dtoValida.setHoraFin(LocalTime.of(12, 0));
                dtoValida.setEstado("activa");

                // Act & Assert
                Reserva resultado = reservaService.crearReserva(dtoValida);
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        // ==================== TESTS DE ERROR - VALIDACIÓN 5 ====================

        @Test
        @DisplayName("❌ VALIDACIÓN 5: Capacidad no disponible")
        void crearReservaSinCapacidad() {
                // Arrange: Cancha con capacidad 1
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha()); // capacidad 1
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                // Ya hay 1 reserva activa
                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(2);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of(reservaExistente));

                // Act & Assert
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(validReservaDTO);
                });

                verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ VALIDACIÓN 5: Capacidad NULL en la cancha (dato legado) se trata como 1, sin NullPointerException")
        void crearReservaConCapacidadNulaSeTrataComoUnaYRechazaSegundaReserva() {
                // Arrange: Cancha sin capacidad definida en la BD (NULL)
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(null);

                // Ya hay 1 reserva activa: con capacidad NULL tratada como 1, debe rechazar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(2);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of(reservaExistente));

                // Act & Assert: BusinessException (409), no NullPointerException (500)
                assertThrows(BusinessException.class, () -> {
                        reservaService.crearReserva(validReservaDTO);
                });

                verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ VALIDACIÓN 5: Capacidad disponible - reserva exitosa")
        void crearReservaConCapacidad() {
                // Arrange: Cancha con capacidad 2
                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(2); // 2 de capacidad

                // Ya hay 1 reserva activa
                Reserva reservaExistente = new Reserva();
                reservaExistente.setUsuarioId(2);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                any(LocalTime.class),
                                any(LocalTime.class),
                                isNull())).thenReturn(List.of(reservaExistente));

                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                // Act
                Reserva resultado = reservaService.crearReserva(validReservaDTO);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Dos usuarios diferentes pueden reservar simultáneamente según la capacidad")
        void dosUsuariosPuedenReservarSimultaneamente() {

                // Arrange
                autenticarUsuario(2, "usuario2@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(2);

                // El usuario 2 no tiene una reserva duplicada
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(2),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of());

                // Ya existe una reserva del usuario 1 en el mismo horario
                Reserva reservaUsuario1 = new Reserva();
                reservaUsuario1.setId(1);
                reservaUsuario1.setUsuarioId(1);
                reservaUsuario1.setCanchaId(1);
                reservaUsuario1.setFecha(LocalDate.of(2026, 9, 10));
                reservaUsuario1.setHoraInicio(LocalTime.of(10, 0));
                reservaUsuario1.setHoraFin(LocalTime.of(11, 0));
                reservaUsuario1.setEstado("activa");

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                isNull())).thenReturn(List.of(reservaUsuario1));

                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                ReservaDTO dtoUsuario2 = new ReservaDTO();
                dtoUsuario2.setUsuarioId(2);
                dtoUsuario2.setCanchaId(1);
                dtoUsuario2.setFecha(LocalDate.of(2026, 9, 10));
                dtoUsuario2.setHoraInicio(LocalTime.of(10, 0));
                dtoUsuario2.setHoraFin(LocalTime.of(11, 0));
                dtoUsuario2.setEstado("activa");

                // Act
                Reserva resultado = reservaService.crearReserva(dtoUsuario2);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Una reserva cancelada no bloquea una nueva reserva")
        void reservaCanceladaNoBloqueaNuevaReserva() {

                // Arrange
                autenticarUsuario(2, "usuario2@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // El usuario 2 no tiene una reserva duplicada
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(2),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull())).thenReturn(List.of());

                // La reserva existente está cancelada,
                // por lo tanto NO debe ocupar capacidad.
                Reserva reservaCancelada = new Reserva();
                reservaCancelada.setId(1);
                reservaCancelada.setUsuarioId(1);
                reservaCancelada.setCanchaId(1);
                reservaCancelada.setFecha(fecha);
                reservaCancelada.setHoraInicio(horaInicio);
                reservaCancelada.setHoraFin(horaFin);
                reservaCancelada.setEstado("cancelada");

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull())).thenReturn(List.of(reservaCancelada));

                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(2);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.crearReserva(dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar una reserva conserva su propio horario")
        void actualizarReservaConservaSuPropioHorario() {

                // Arrange
                Integer reservaId = 1;

                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                // La propia reserva se excluye mediante su ID.
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                eq(reservaId))).thenReturn(List.of());

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                eq(reservaId))).thenReturn(List.of());

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(reservaExistente);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(LocalDate.of(2026, 9, 10));
                dto.setHoraInicio(LocalTime.of(10, 0));
                dto.setHoraFin(LocalTime.of(11, 0));
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.actualizarReserva(reservaId, dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar reserva rechaza horario duplicado del mismo usuario")
        void actualizarReservaRechazaHorarioDuplicado() {

                // Arrange
                Integer reservaId = 1;

                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(1);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(LocalDate.of(2026, 9, 10));
                otraReserva.setHoraInicio(LocalTime.of(11, 0));
                otraReserva.setHoraFin(LocalTime.of(12, 0));
                otraReserva.setEstado("activa");

                // La otra reserva del mismo usuario entra en conflicto
                // con el nuevo horario 11:30 - 12:30.
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 30)),
                                eq(LocalTime.of(12, 30)),
                                eq(reservaId))).thenReturn(List.of(otraReserva));

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(LocalDate.of(2026, 9, 10));
                dto.setHoraInicio(LocalTime.of(11, 30));
                dto.setHoraFin(LocalTime.of(12, 30));
                dto.setEstado("activa");

                // Act + Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(reservaId, dto));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar reserva rechaza horario sin capacidad")
        void actualizarReservaRechazaHorarioSinCapacidad() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                // No existe duplicado del mismo usuario.
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 0)),
                                eq(LocalTime.of(12, 0)),
                                eq(reservaId))).thenReturn(List.of());

                // La capacidad ya está ocupada por otra reserva.
                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(2);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(LocalDate.of(2026, 9, 10));
                otraReserva.setHoraInicio(LocalTime.of(11, 0));
                otraReserva.setHoraFin(LocalTime.of(12, 0));
                otraReserva.setEstado("activa");

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 0)),
                                eq(LocalTime.of(12, 0)),
                                eq(reservaId))).thenReturn(List.of(otraReserva));

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(LocalDate.of(2026, 9, 10));
                dto.setHoraInicio(LocalTime.of(11, 0));
                dto.setHoraFin(LocalTime.of(12, 0));
                dto.setEstado("activa");

                // Act + Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(reservaId, dto));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Una reserva cancelada no bloquea el mismo horario")
        void reservaCanceladaNoBloqueaHorario() {

                // Arrange
                autenticarUsuario(2, "usuario2@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // El usuario 2 no tiene una reserva duplicada
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(2),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull())).thenReturn(List.of());

                // Existe una reserva en ese horario, pero está CANCELADA
                Reserva reservaCancelada = new Reserva();
                reservaCancelada.setId(1);
                reservaCancelada.setUsuarioId(1);
                reservaCancelada.setCanchaId(1);
                reservaCancelada.setFecha(fecha);
                reservaCancelada.setHoraInicio(horaInicio);
                reservaCancelada.setHoraFin(horaFin);
                reservaCancelada.setEstado("cancelada");

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull())).thenReturn(List.of(reservaCancelada));

                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(2);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.crearReserva(dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar una reserva no genera conflicto consigo misma")
        void actualizarReservaNoGeneraConflictoConsigoMisma() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(1);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(LocalDate.of(2026, 9, 10));
                reservaExistente.setHoraInicio(LocalTime.of(10, 0));
                reservaExistente.setHoraFin(LocalTime.of(11, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(1))
                                .thenReturn(Optional.of(reservaExistente));

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                // No debe encontrar otra reserva duplicada.
                // El ID 1 se excluye de la búsqueda.
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                eq(1))).thenReturn(List.of());

                // La propia reserva también se excluye de la búsqueda de capacidad.
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(10, 0)),
                                eq(LocalTime.of(11, 0)),
                                eq(1))).thenReturn(List.of());

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(reservaExistente);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(LocalDate.of(2026, 9, 10));
                dto.setHoraInicio(LocalTime.of(10, 0));
                dto.setHoraFin(LocalTime.of(11, 0));
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.actualizarReserva(1, dto);

                // Assert
                assertNotNull(resultado);
                assertEquals(1, resultado.getId());
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar una reserva rechaza horario ocupado por otra reserva")
        void actualizarReservaRechazaHorarioOcupado() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reservaActual = new Reserva();
                reservaActual.setId(1);
                reservaActual.setUsuarioId(1);
                reservaActual.setCanchaId(1);
                reservaActual.setFecha(LocalDate.of(2026, 9, 10));
                reservaActual.setHoraInicio(LocalTime.of(10, 0));
                reservaActual.setHoraFin(LocalTime.of(11, 0));
                reservaActual.setEstado("activa");

                when(reservaRepository.findById(1))
                                .thenReturn(Optional.of(reservaActual));

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(2);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(LocalDate.of(2026, 9, 10));
                otraReserva.setHoraInicio(LocalTime.of(11, 0));
                otraReserva.setHoraFin(LocalTime.of(12, 0));
                otraReserva.setEstado("activa");

                // El horario 11:30-12:30 se superpone con la reserva del usuario 2
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 30)),
                                eq(LocalTime.of(12, 30)),
                                eq(1))).thenReturn(List.of());

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(LocalDate.of(2026, 9, 10)),
                                eq(LocalTime.of(11, 30)),
                                eq(LocalTime.of(12, 30)),
                                eq(1))).thenReturn(List.of(otraReserva));

                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(LocalDate.of(2026, 9, 10));
                dto.setHoraInicio(LocalTime.of(11, 30));
                dto.setHoraFin(LocalTime.of(12, 30));
                dto.setEstado("activa");

                // Act + Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(1, dto));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Actualizar reserva exitosamente")
        void actualizarReservaExitosa() {

                // Arrange
                Integer reservaId = 1;

                autenticarUsuario(1, "usuario@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // Reserva que ya existe y que vamos a actualizar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(LocalTime.of(9, 0));
                reservaExistente.setHoraFin(LocalTime.of(10, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // La propia reserva no genera duplicado
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                // No hay otras reservas superpuestas
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(reservaExistente);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.actualizarReserva(reservaId, dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(reservaExistente);
        }

        @Test
        @DisplayName("No permite actualizar una reserva a un horario duplicado")
        void actualizarReservaDuplicada() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // Reserva que estamos intentando actualizar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(LocalTime.of(8, 0));
                reservaExistente.setHoraFin(LocalTime.of(9, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // Existe OTRA reserva del mismo usuario en el horario solicitado
                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(1);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(fecha);
                otraReserva.setHoraInicio(horaInicio);
                otraReserva.setHoraFin(horaFin);
                otraReserva.setEstado("activa");

                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of(otraReserva));

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act + Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(reservaId, dto));

                // No debe guardar la reserva
                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("No permite actualizar una reserva cuando se supera la capacidad")
        void actualizarReservaSinCapacidad() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // Reserva que estamos intentando actualizar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(LocalTime.of(8, 0));
                reservaExistente.setHoraFin(LocalTime.of(9, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // El usuario 1 no tiene otra reserva duplicada
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                // Ya existe otra reserva ocupando el horario solicitado
                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(2);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(fecha);
                otraReserva.setHoraInicio(horaInicio);
                otraReserva.setHoraFin(horaFin);
                otraReserva.setEstado("activa");

                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of(otraReserva));

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act & Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(reservaId, dto));

                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Permite actualizar una reserva cuando hay capacidad disponible")
        void actualizarReservaConCapacidad() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // Reserva que estamos intentando actualizar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(LocalTime.of(8, 0));
                reservaExistente.setHoraFin(LocalTime.of(9, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // No existe otra reserva duplicada del mismo usuario
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                // No existe otra reserva ocupando el horario solicitado
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(reservaExistente);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.actualizarReserva(reservaId, dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Una reserva cancelada no bloquea una nueva reserva")
        void crearReservaConReservaCancelada() {

                // Arrange
                autenticarUsuario(2, "usuario2@test.com", "USER");

                when(canchaService.obtenerCancha(1)).thenReturn(createMockCancha());
                when(canchaService.obtenerCapacidadCancha(1)).thenReturn(1);

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                // El usuario 2 no tiene una reserva duplicada
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(2),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull()))
                                .thenReturn(List.of());

                // La reserva existente está cancelada,
                // por lo tanto NO debe bloquear el horario.
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                isNull()))
                                .thenReturn(List.of());

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(mockReserva);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(2);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.crearReserva(dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Permite actualizar una reserva sin considerarla duplicada a sí misma")
        void actualizarReservaSinDuplicarse() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                when(canchaService.obtenerCancha(1))
                                .thenReturn(createMockCancha());

                // Reserva que estamos actualizando
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(horaInicio);
                reservaExistente.setHoraFin(horaFin);
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // La propia reserva se excluye mediante reservaId = 1
                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                // La propia reserva también se excluye de la validación de capacidad
                when(reservaRepository.buscarReservasSuperpuestas(
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of());

                when(canchaService.obtenerCapacidadCancha(1))
                                .thenReturn(1);

                when(reservaRepository.save(any(Reserva.class)))
                                .thenReturn(reservaExistente);

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act
                Reserva resultado = reservaService.actualizarReserva(reservaId, dto);

                // Assert
                assertNotNull(resultado);
                verify(reservaRepository).save(reservaExistente);
        }

        @Test
        @DisplayName("No permite actualizar una reserva si genera duplicidad con otra")
        void actualizarReservaGeneraDuplicidad() {

                // Arrange
                autenticarUsuario(1, "usuario@test.com", "USER");

                Integer reservaId = 1;

                LocalDate fecha = LocalDate.of(2026, 9, 10);
                LocalTime horaInicio = LocalTime.of(10, 0);
                LocalTime horaFin = LocalTime.of(11, 0);

                when(canchaService.obtenerCancha(1))
                                .thenReturn(createMockCancha());

                // Reserva que estamos intentando actualizar
                Reserva reservaExistente = new Reserva();
                reservaExistente.setId(reservaId);
                reservaExistente.setUsuarioId(1);
                reservaExistente.setCanchaId(1);
                reservaExistente.setFecha(fecha);
                reservaExistente.setHoraInicio(LocalTime.of(8, 0));
                reservaExistente.setHoraFin(LocalTime.of(9, 0));
                reservaExistente.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reservaExistente));

                // Otra reserva del mismo usuario que genera conflicto
                Reserva otraReserva = new Reserva();
                otraReserva.setId(2);
                otraReserva.setUsuarioId(1);
                otraReserva.setCanchaId(1);
                otraReserva.setFecha(fecha);
                otraReserva.setHoraInicio(horaInicio);
                otraReserva.setHoraFin(horaFin);
                otraReserva.setEstado("activa");

                when(reservaRepository.buscarReservasDuplicadas(
                                eq(1),
                                eq(1),
                                eq(fecha),
                                eq(horaInicio),
                                eq(horaFin),
                                eq(reservaId)))
                                .thenReturn(List.of(otraReserva));

                ReservaDTO dto = new ReservaDTO();
                dto.setUsuarioId(1);
                dto.setCanchaId(1);
                dto.setFecha(fecha);
                dto.setHoraInicio(horaInicio);
                dto.setHoraFin(horaFin);
                dto.setEstado("activa");

                // Act + Assert
                assertThrows(
                                BusinessException.class,
                                () -> reservaService.actualizarReserva(reservaId, dto));

                // No debe guardar porque la actualización fue rechazada
                verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Lista todas las reservas correctamente")
        void listarReservas() {

                // Arrange
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                "usuario@test.com",
                                null,
                                java.util.List.of(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_ADMIN")));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                Reserva reserva1 = new Reserva();
                reserva1.setId(1);
                reserva1.setUsuarioId(1);
                reserva1.setCanchaId(1);
                reserva1.setFecha(LocalDate.of(2026, 9, 10));
                reserva1.setHoraInicio(LocalTime.of(10, 0));
                reserva1.setHoraFin(LocalTime.of(11, 0));
                reserva1.setEstado("activa");

                Reserva reserva2 = new Reserva();
                reserva2.setId(2);
                reserva2.setUsuarioId(2);
                reserva2.setCanchaId(1);
                reserva2.setFecha(LocalDate.of(2026, 9, 10));
                reserva2.setHoraInicio(LocalTime.of(11, 0));
                reserva2.setHoraFin(LocalTime.of(12, 0));
                reserva2.setEstado("activa");

                when(reservaRepository.findAll())
                                .thenReturn(List.of(reserva1, reserva2));

                // Act
                List<Reserva> resultado = reservaService.listarReservas();

                // Assert
                assertNotNull(resultado);
                assertEquals(2, resultado.size());
                assertEquals(1, resultado.get(0).getId());
                assertEquals(2, resultado.get(1).getId());

                verify(reservaRepository).findAll();
        }

        @Test
        @DisplayName("Un ADMIN puede cancelar la reserva de otro usuario")
        void adminPuedeCancelarReservaDeOtroUsuario() {
                // Arrange
                autenticarUsuario(99, "admin@test.com", "ADMIN");

                when(reservaRepository.findById(1)).thenReturn(Optional.of(mockReserva));
                when(reservaRepository.save(any(Reserva.class))).thenReturn(mockReserva);

                // Act
                Reserva resultado = reservaService.cancelarReserva(1);

                // Assert
                assertEquals("cancelada", resultado.getEstado());
                verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Elimina una reserva existente correctamente (dueño autenticado)")
        void eliminarReserva() {

                // Arrange
                Integer reservaId = 1;
                autenticarUsuario(1, "usuario@test.com", "USER");

                Reserva reserva = new Reserva();
                reserva.setId(reservaId);
                reserva.setUsuarioId(1);
                reserva.setCanchaId(1);
                reserva.setFecha(LocalDate.of(2026, 9, 10));
                reserva.setHoraInicio(LocalTime.of(10, 0));
                reserva.setHoraFin(LocalTime.of(11, 0));
                reserva.setEstado("activa");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(reserva));

                // Act
                reservaService.eliminarReserva(reservaId);

                // Assert
                verify(reservaRepository).findById(reservaId);
                verify(reservaRepository).deleteById(reservaId);
        }

        @Test
        @DisplayName("No permite eliminar una reserva que no existe")
        void eliminarReservaNoExiste() {

                // Arrange
                Integer reservaId = 999;

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(ResourceNotFoundException.class, () -> {
                        reservaService.eliminarReserva(reservaId);
                });

                verify(reservaRepository).findById(reservaId);
                verify(reservaRepository, never()).deleteById(reservaId);
        }

        @Test
        @DisplayName("Un usuario no puede eliminar la reserva de otro usuario")
        void usuarioNoPuedeEliminarReservaDeOtroUsuario() {

                // Arrange
                Integer reservaId = 1;
                autenticarUsuario(2, "otro@test.com", "USER");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(mockReserva)); // mockReserva pertenece al usuario 1

                // Act & Assert
                assertThrows(
                                org.springframework.security.access.AccessDeniedException.class,
                                () -> reservaService.eliminarReserva(reservaId));

                verify(reservaRepository, never()).deleteById(reservaId);
        }

        @Test
        @DisplayName("Un ADMIN puede eliminar la reserva de otro usuario")
        void adminPuedeEliminarReservaDeOtroUsuario() {

                // Arrange
                Integer reservaId = 1;
                autenticarUsuario(99, "admin@test.com", "ADMIN");

                when(reservaRepository.findById(reservaId))
                                .thenReturn(Optional.of(mockReserva)); // mockReserva pertenece al usuario 1

                // Act
                reservaService.eliminarReserva(reservaId);

                // Assert
                verify(reservaRepository).deleteById(reservaId);
        }

        // ==================== HELPER METHODS ====================

        private com.sportcourt.backend.model.Cancha createMockCancha() {
                com.sportcourt.backend.model.Cancha cancha = new com.sportcourt.backend.model.Cancha();
                cancha.setId(1);
                cancha.setCapacity(1);
                return cancha;
        }
}
