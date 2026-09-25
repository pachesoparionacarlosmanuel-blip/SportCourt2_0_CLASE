package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.repository.ClaseRepository;
import com.sportcourt.backend.service.InscripcionService;

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

/**
 * Test de integración HTTP real (sin mocks) para /api/inscripciones:
 * controller -> service -> repository -> H2. Cubre creación, duplicados,
 * cupos, 404 y autorización basada en dueño.
 *
 * NOTA: al igual que en ReservaControllerIT, las comprobaciones de dueño
 * (InscripcionController#buscarInscripcion e
 * InscripcionService#cancelarInscripcion/#eliminarInscripcion) lanzan
 * AccessDeniedException manualmente desde dentro del controller/service. Esa
 * excepción no tiene un @ExceptionHandler específico en
 * GlobalExceptionHandler y termina en el manejador genérico -> 500, en vez
 * de 403. Comprobado empíricamente. Ver ReservaControllerIT para más
 * detalle; no se modifica GlobalExceptionHandler porque esta tarea es solo
 * de tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InscripcionControllerTest extends AbstractControllerTest {

    @Autowired
    private ClaseRepository claseRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    private Clase crearClase(int cupos) {
        Clase clase = new Clase();
        clase.setName("Clase Inscripciones " + java.util.UUID.randomUUID());
        clase.setIcon("🏀");
        clase.setLevel("todos");
        clase.setSchedule("Miércoles 17:00");
        clase.setProfessor("Prof. Test");
        clase.setPrice(15.0);
        clase.setSlots(cupos);
        return claseRepository.save(clase);
    }

    private Map<String, Object> inscripcionDto(Integer claseId) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("claseId", claseId);
        dto.put("fecha", LocalDate.now().plusDays(2).format(DATE_FMT));
        dto.put("estado", "activa");
        return dto;
    }

    @Test
    @DisplayName("POST /api/inscripciones autenticado crea la inscripción y fuerza el usuarioId (201)")
    void crearInscripcionExitosa() throws Exception {
        String email = uniqueEmail("inscripcion-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(5);

        HttpResponse<String> response = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));

        assertEquals(201, response.statusCode(), response.body());
        JsonNode body = json(response);
        assertEquals(clase.getId(), body.get("claseId").asInt());
        assertEquals(InscripcionService.ESTADO_INICIAL, body.get("estado").asText());
    }

    @Test
    @DisplayName("POST /api/inscripciones sin claseId devuelve 400 (validación)")
    void crearInscripcionSinClaseIdDevuelve400() throws Exception {
        String email = uniqueEmail("inscripcion-invalida");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("fecha", LocalDate.now().plusDays(1).format(DATE_FMT));
        dto.put("estado", "activa");
        // claseId omitido a propósito

        HttpResponse<String> response = mutate(session, "POST", "/api/inscripciones", dto);

        assertEquals(400, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Validation Error", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/inscripciones con clase inexistente devuelve 404")
    void crearInscripcionConClaseInexistenteDevuelve404() throws Exception {
        String email = uniqueEmail("inscripcion-clase-404");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = mutate(session, "POST", "/api/inscripciones", inscripcionDto(999999));

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("POST /api/inscripciones duplicada (mismo usuario, misma clase) devuelve 409")
    void crearInscripcionDuplicadaDevuelve409() throws Exception {
        String email = uniqueEmail("inscripcion-duplicada");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(5); // cupos amplios: aísla la validación de duplicado

        HttpResponse<String> primera = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        assertEquals(201, primera.statusCode(), primera.body());

        HttpResponse<String> segunda = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));

        assertEquals(409, segunda.statusCode(), segunda.body());
        JsonNode body = json(segunda);
        assertEquals("Business Logic Error", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/inscripciones sin cupos disponibles devuelve 409")
    void crearInscripcionSinCuposDevuelve409() throws Exception {
        Clase clase = crearClase(1); // 1 solo cupo

        String email1 = uniqueEmail("inscripcion-cupo-1");
        crearUsuario(email1, "UserPass123", "usuario");
        Session session1 = login(email1, "UserPass123");

        HttpResponse<String> primera = mutate(session1, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        assertEquals(201, primera.statusCode(), primera.body());

        String email2 = uniqueEmail("inscripcion-cupo-2");
        crearUsuario(email2, "UserPass123", "usuario");
        Session session2 = login(email2, "UserPass123");

        // Usuario DIFERENTE (no es duplicado) pero ya no hay cupos (1/1 ocupado)
        HttpResponse<String> segunda = mutate(session2, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));

        assertEquals(409, segunda.statusCode(), segunda.body());
        JsonNode body = json(segunda);
        assertEquals("Business Logic Error", body.get("error").asText());
    }

    @Test
    @DisplayName("GET /api/inscripciones/{id} como dueño devuelve 200")
    void obtenerInscripcionComoDueño() throws Exception {
        String email = uniqueEmail("inscripcion-owner-get");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = get(session, "/api/inscripciones/" + id);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals(id, body.get("id").asInt());
    }

    @Test
    @DisplayName("GET /api/inscripciones/{id} inexistente devuelve 404")
    void obtenerInscripcionInexistenteDevuelve404() throws Exception {
        String email = uniqueEmail("inscripcion-404");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = get(session, "/api/inscripciones/999999");

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/inscripciones/{id} de otro usuario devuelve 403 (autorización por propietario)")
    void obtenerInscripcionDeOtroUsuarioDevuelve403() throws Exception {
        String emailDueño = uniqueEmail("inscripcion-owner-x");
        crearUsuario(emailDueño, "UserPass123", "usuario");
        Session sesionDueño = login(emailDueño, "UserPass123");

        Clase clase = crearClase(5);
        HttpResponse<String> creada = mutate(sesionDueño, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        int id = json(creada).get("id").asInt();

        String emailOtro = uniqueEmail("inscripcion-otro");
        crearUsuario(emailOtro, "UserPass123", "usuario");
        Session sesionOtro = login(emailOtro, "UserPass123");

        HttpResponse<String> response = get(sesionOtro, "/api/inscripciones/" + id);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("PUT /api/inscripciones/{id}/cancelar como dueño cancela la inscripción (200)")
    void cancelarInscripcionComoDueño() throws Exception {
        String email = uniqueEmail("inscripcion-cancelar-owner");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = mutate(session, "PUT", "/api/inscripciones/" + id + "/cancelar", null);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("cancelada", body.get("estado").asText());
    }

    @Test
    @DisplayName("DELETE /api/inscripciones/{id} de otro usuario (no ADMIN) devuelve 403 (autorización por propietario)")
    void eliminarInscripcionDeOtroUsuarioDevuelve403() throws Exception {
        String emailDueño = uniqueEmail("inscripcion-delete-owner");
        crearUsuario(emailDueño, "UserPass123", "usuario");
        Session sesionDueño = login(emailDueño, "UserPass123");

        Clase clase = crearClase(5);
        HttpResponse<String> creada = mutate(sesionDueño, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        int id = json(creada).get("id").asInt();

        String emailOtro = uniqueEmail("inscripcion-delete-otro");
        crearUsuario(emailOtro, "UserPass123", "usuario");
        Session sesionOtro = login(emailOtro, "UserPass123");

        HttpResponse<String> response = mutate(sesionOtro, "DELETE", "/api/inscripciones/" + id, null);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("DELETE /api/inscripciones/{id} como dueño elimina la inscripción (204)")
    void eliminarInscripcionComoDueño() throws Exception {
        String email = uniqueEmail("inscripcion-delete-owner-ok");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(5);
        HttpResponse<String> creada = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        int id = json(creada).get("id").asInt();

        HttpResponse<String> response = mutate(session, "DELETE", "/api/inscripciones/" + id, null);

        assertEquals(204, response.statusCode());
    }

    @Test
    @DisplayName("Tras cancelar, el usuario puede volver a inscribirse (la consulta ignora canceladas en BD)")
    void reinscribirseTrasCancelar() throws Exception {
        String email = uniqueEmail("inscripcion-reinscribir");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Clase clase = crearClase(1);
        HttpResponse<String> creada = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        assertEquals(201, creada.statusCode(), creada.body());
        int id = json(creada).get("id").asInt();

        // Mientras está activa: duplicada -> 409
        HttpResponse<String> duplicada = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        assertEquals(409, duplicada.statusCode());

        mutate(session, "PUT", "/api/inscripciones/" + id + "/cancelar", null);

        // Cancelada: ya no cuenta como duplicado ni ocupa el único cupo
        HttpResponse<String> nueva = mutate(session, "POST", "/api/inscripciones", inscripcionDto(clase.getId()));
        assertEquals(201, nueva.statusCode(), nueva.body());
    }
}
