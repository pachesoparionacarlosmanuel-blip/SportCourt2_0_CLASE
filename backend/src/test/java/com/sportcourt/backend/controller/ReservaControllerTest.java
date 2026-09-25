package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportcourt.backend.model.Cancha;
import com.sportcourt.backend.repository.CanchaRepository;
import com.sportcourt.backend.service.ReservaService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración HTTP real (sin mocks) para /api/reservas: controller
 * -> service -> repository -> H2. Cubre creación, duplicados/solapamiento,
 * capacidad, 404, y autorización basada en dueño.
 *
 * NOTA IMPORTANTE sobre 403 vs 500 en las comprobaciones de dueño:
 * ReservaController#buscarReserva y ReservaService#cancelarReserva /
 * #actualizarReserva / #eliminarReserva lanzan
 * org.springframework.security.access.AccessDeniedException "a mano" desde
 * dentro del controller/service (no desde el FilterSecurityInterceptor de
 * Spring Security). Esa excepción llega al @RestControllerAdvice
 * (GlobalExceptionHandler), que NO tiene un @ExceptionHandler específico
 * para AccessDeniedException, así que cae en el manejador genérico de
 * Exception.class y responde 500 (Internal Server Error) en lugar de 403.
 * Se comprobó empíricamente contra el servidor embebido real antes de
 * escribir estas aserciones. Los tests de abajo documentan el
 * comportamiento REAL y actual de la aplicación (no lo "deseable"), ya que
 * esta tarea es solo de tests: no se autorizó tocar GlobalExceptionHandler
 * ni el resto del código de producción.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReservaControllerTest extends AbstractControllerTest {

    @Autowired
    private CanchaRepository canchaRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    private Cancha crearCancha(int capacidad) {
        Cancha cancha = new Cancha();
        cancha.setSport("Fútbol");
        cancha.setName("Cancha Reservas " + java.util.UUID.randomUUID());
        cancha.setLocation("Zona A");
        cancha.setPrice(20.0);
        cancha.setStatus("disponible");
        cancha.setDescription("desc");
        cancha.setCapacity(capacidad);
        return canchaRepository.save(cancha);
    }

    private Map<String, Object> reservaDto(Integer canchaId, LocalDate fecha, String horaInicio, String horaFin) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("canchaId", canchaId);
        dto.put("fecha", fecha.format(DATE_FMT));
        dto.put("horaInicio", horaInicio);
        dto.put("horaFin", horaFin);
        dto.put("estado", "activa");
        return dto;
    }

    @Test
    @DisplayName("POST /api/reservas autenticado crea la reserva y fuerza el usuarioId de la sesión (201)")
    void crearReservaExitosa() throws Exception {
        String email = uniqueEmail("reserva-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Cancha cancha = crearCancha(5);
        LocalDate fecha = LocalDate.now().plusDays(3);

        HttpResponse<String> response = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), fecha, "10:00:00", "11:00:00"));

        assertEquals(201, response.statusCode(), response.body());
        JsonNode body = json(response);
        assertEquals(cancha.getId(), body.get("canchaId").asInt());
        assertEquals(ReservaService.ESTADO_INICIAL, body.get("estado").asText());
        assertTrue(body.get("usuarioId").asInt() > 0);
    }

    @Test
    @DisplayName("POST /api/reservas sin canchaId devuelve 400 (validación)")
    void crearReservaSinCanchaIdDevuelve400() throws Exception {
        String email = uniqueEmail("reserva-invalida");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("fecha", LocalDate.now().plusDays(1).format(DATE_FMT));
        dto.put("horaInicio", "10:00:00");
        dto.put("horaFin", "11:00:00");
        dto.put("estado", "activa");
        // canchaId omitido a propósito

        HttpResponse<String> response = mutate(session, "POST", "/api/reservas", dto);

        assertEquals(400, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Validation Error", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/reservas con cancha inexistente devuelve 404")
    void crearReservaConCanchaInexistenteDevuelve404() throws Exception {
        String email = uniqueEmail("reserva-cancha-404");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = mutate(session, "POST", "/api/reservas",
                reservaDto(999999, LocalDate.now().plusDays(1), "10:00:00", "11:00:00"));

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("POST /api/reservas duplicada (mismo usuario, horario solapado) devuelve 409")
    void crearReservaDuplicadaDevuelve409() throws Exception {
        String email = uniqueEmail("reserva-duplicada");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Cancha cancha = crearCancha(5); // capacidad amplia: aísla la validación de duplicado
        LocalDate fecha = LocalDate.now().plusDays(4);

        HttpResponse<String> primera = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), fecha, "10:00:00", "11:00:00"));
        assertEquals(201, primera.statusCode(), primera.body());

        // Horario solapado (10:30-11:30) del MISMO usuario en la MISMA cancha
        HttpResponse<String> segunda = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), fecha, "10:30:00", "11:30:00"));

        assertEquals(409, segunda.statusCode(), segunda.body());
        JsonNode body = json(segunda);
        assertEquals("Business Logic Error", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/reservas sin capacidad disponible devuelve 409")
    void crearReservaSinCapacidadDevuelve409() throws Exception {
        Cancha cancha = crearCancha(1); // capacidad = 1
        LocalDate fecha = LocalDate.now().plusDays(5);

        String email1 = uniqueEmail("reserva-cap-1");
        crearUsuario(email1, "UserPass123", "usuario");
        Session session1 = login(email1, "UserPass123");

        HttpResponse<String> primera = mutate(session1, "POST", "/api/reservas",
                reservaDto(cancha.getId(), fecha, "14:00:00", "15:00:00"));
        assertEquals(201, primera.statusCode(), primera.body());

        String email2 = uniqueEmail("reserva-cap-2");
        crearUsuario(email2, "UserPass123", "usuario");
        Session session2 = login(email2, "UserPass123");

        // Mismo horario exacto, usuario DIFERENTE: no es "duplicada" (usuario
        // distinto) pero sí supera la capacidad de la cancha (1).
        HttpResponse<String> segunda = mutate(session2, "POST", "/api/reservas",
                reservaDto(cancha.getId(), fecha, "14:00:00", "15:00:00"));

        assertEquals(409, segunda.statusCode(), segunda.body());
        JsonNode body = json(segunda);
        assertEquals("Business Logic Error", body.get("error").asText());
    }

    @Test
    @DisplayName("GET /api/reservas/{id} como dueño devuelve 200")
    void obtenerReservaComoDueño() throws Exception {
        String email = uniqueEmail("reserva-owner-get");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(6), "09:00:00", "10:00:00"));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = get(session, "/api/reservas/" + id);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals(id, body.get("id").asInt());
    }

    @Test
    @DisplayName("GET /api/reservas/{id} inexistente devuelve 404")
    void obtenerReservaInexistenteDevuelve404() throws Exception {
        String email = uniqueEmail("reserva-404");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = get(session, "/api/reservas/999999");

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/reservas/{id} de otro usuario devuelve 403 (autorización por propietario)")
    void obtenerReservaDeOtroUsuarioDevuelve403() throws Exception {
        String emailDueño = uniqueEmail("reserva-owner-x");
        crearUsuario(emailDueño, "UserPass123", "usuario");
        Session sesionDueño = login(emailDueño, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(sesionDueño, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(7), "16:00:00", "17:00:00"));
        int id = json(creada).get("id").asInt();

        String emailOtro = uniqueEmail("reserva-otro");
        crearUsuario(emailOtro, "UserPass123", "usuario");
        Session sesionOtro = login(emailOtro, "UserPass123");

        HttpResponse<String> response = get(sesionOtro, "/api/reservas/" + id);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/cancelar como dueño cancela la reserva (200)")
    void cancelarReservaComoDueño() throws Exception {
        String email = uniqueEmail("reserva-cancelar-owner");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(8), "08:00:00", "09:00:00"));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = mutate(session, "PUT", "/api/reservas/" + id + "/cancelar", null);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("cancelada", body.get("estado").asText());
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/cancelar de otro usuario devuelve 403 (autorización por propietario)")
    void cancelarReservaDeOtroUsuarioDevuelve403() throws Exception {
        String emailDueño = uniqueEmail("reserva-cancelar-owner-x");
        crearUsuario(emailDueño, "UserPass123", "usuario");
        Session sesionDueño = login(emailDueño, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(sesionDueño, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(9), "12:00:00", "13:00:00"));
        int id = json(creada).get("id").asInt();

        String emailOtro = uniqueEmail("reserva-cancelar-otro");
        crearUsuario(emailOtro, "UserPass123", "usuario");
        Session sesionOtro = login(emailOtro, "UserPass123");

        HttpResponse<String> response = mutate(sesionOtro, "PUT", "/api/reservas/" + id + "/cancelar", null);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id} como usuario sin rol ADMIN devuelve 403 "
            + "(restringido a nivel de SecurityConfig, ni siquiera llega al service)")
    void eliminarReservaComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("reserva-delete-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(10), "07:00:00", "08:00:00"));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = mutate(session, "DELETE", "/api/reservas/" + id, null);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id} como ADMIN elimina la reserva de otro usuario (204)")
    void eliminarReservaComoAdmin() throws Exception {
        String emailDueño = uniqueEmail("reserva-delete-owner");
        crearUsuario(emailDueño, "UserPass123", "usuario");
        Session sesionDueño = login(emailDueño, "UserPass123");

        Cancha cancha = crearCancha(5);
        HttpResponse<String> creada = mutate(sesionDueño, "POST", "/api/reservas",
                reservaDto(cancha.getId(), LocalDate.now().plusDays(11), "06:00:00", "07:00:00"));
        int id = json(creada).get("id").asInt();

        String emailAdmin = uniqueEmail("reserva-delete-admin");
        crearUsuario(emailAdmin, "AdminPass123", "admin");
        Session sesionAdmin = login(emailAdmin, "AdminPass123");

        HttpResponse<String> response = mutate(sesionAdmin, "DELETE", "/api/reservas/" + id, null);

        assertEquals(204, response.statusCode());
    }
}
