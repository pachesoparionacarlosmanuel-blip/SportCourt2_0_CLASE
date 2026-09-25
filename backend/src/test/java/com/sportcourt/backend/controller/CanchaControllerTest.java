package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportcourt.backend.model.Cancha;
import com.sportcourt.backend.repository.CanchaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración HTTP real (sin mocks) para /api/canchas: controller ->
 * service -> repository -> H2. Sigue el patrón de CsrfLoginFlowTest (raw
 * HttpClient, sin spring-security-test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CanchaControllerTest extends AbstractControllerTest {

    @Autowired
    private CanchaRepository canchaRepository;

    private Cancha canchaSeed;

    @BeforeEach
    void seedCancha() {
        Cancha cancha = new Cancha();
        cancha.setSport("Fútbol");
        cancha.setName("Cancha Central");
        cancha.setLocation("Zona A");
        cancha.setPrice(50.0);
        cancha.setStatus("disponible");
        cancha.setDescription("Cancha de fútbol con piso sintético");
        cancha.setCapacity(4);
        canchaSeed = canchaRepository.save(cancha);
    }

    @Test
    @DisplayName("GET /api/canchas es público y devuelve la lista")
    void listarCanchasEsPublico() throws Exception {
        HttpResponse<String> response = get(null, "/api/canchas");

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertTrue(body.isArray());
        boolean contieneSeed = false;
        for (JsonNode nodo : body) {
            if (nodo.get("id").asInt() == canchaSeed.getId()) {
                contieneSeed = true;
            }
        }
        assertTrue(contieneSeed, "La lista pública debería incluir la cancha creada");
    }

    @Test
    @DisplayName("GET /api/canchas/{id} público devuelve la cancha correcta")
    void buscarCanchaPorIdEsPublico() throws Exception {
        HttpResponse<String> response = get(null, "/api/canchas/" + canchaSeed.getId());

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Cancha Central", body.get("name").asText());
        assertEquals("Fútbol", body.get("sport").asText());
    }

    @Test
    @DisplayName("GET /api/canchas/{id} inexistente devuelve 404 con el ErrorResponse estándar")
    void buscarCanchaInexistenteDevuelve404() throws Exception {
        HttpResponse<String> response = get(null, "/api/canchas/999999");

        assertEquals(404, response.statusCode());
        JsonNode body = json(response);
        assertEquals(404, body.get("status").asInt());
        assertEquals("Not Found", body.get("error").asText());
        assertTrue(body.has("message"));
    }

    @Test
    @DisplayName("POST /api/canchas como ADMIN crea la cancha (201)")
    void crearCanchaComoAdmin() throws Exception {
        String email = uniqueEmail("cancha-admin");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        Map<String, Object> dto = Map.of(
                "sport", "Vóley",
                "name", "Cancha Nueva",
                "location", "Zona B",
                "price", 30.0,
                "status", "disponible",
                "description", "Cancha techada",
                "capacity", 6);

        HttpResponse<String> response = mutate(session, "POST", "/api/canchas", dto);

        assertEquals(201, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Cancha Nueva", body.get("name").asText());
        assertEquals("Vóley", body.get("sport").asText());
        assertTrue(body.get("id").asInt() > 0);
    }

    @Test
    @DisplayName("POST /api/canchas con campos requeridos vacíos devuelve 400")
    void crearCanchaConDatosInvalidosDevuelve400() throws Exception {
        String email = uniqueEmail("cancha-admin-invalido");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        // "name" y "location" en blanco disparan @NotBlank; "price" y
        // "capacity" en 0 disparan @Positive.
        Map<String, Object> dtoInvalido = Map.of(
                "sport", "",
                "name", "",
                "location", "",
                "price", 0,
                "status", "",
                "description", "",
                "capacity", 0);

        HttpResponse<String> response = mutate(session, "POST", "/api/canchas", dtoInvalido);

        assertEquals(400, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Validation Error", body.get("error").asText());
        assertTrue(body.get("message").asText().contains("requerid"),
                "El mensaje debería mencionar los campos requeridos: " + body.get("message"));
    }

    @Test
    @DisplayName("POST /api/canchas sin sesión es rechazado (403, filtro CSRF/seguridad)")
    void crearCanchaSinSesionEsRechazada() throws Exception {
        Map<String, Object> dto = Map.of(
                "sport", "Fútbol", "name", "X", "location", "Y",
                "price", 10.0, "status", "disponible", "description", "d", "capacity", 1);

        HttpResponse<String> response = mutate(null, "POST", "/api/canchas", dto);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("POST /api/canchas como usuario sin rol ADMIN devuelve 403")
    void crearCanchaComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("cancha-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Map<String, Object> dto = Map.of(
                "sport", "Fútbol", "name", "X", "location", "Y",
                "price", 10.0, "status", "disponible", "description", "d", "capacity", 1);

        HttpResponse<String> response = mutate(session, "POST", "/api/canchas", dto);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("PUT /api/canchas/{id} como ADMIN actualiza la cancha")
    void actualizarCanchaComoAdmin() throws Exception {
        String email = uniqueEmail("cancha-admin-put");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        Map<String, Object> dto = Map.of(
                "sport", "Fútbol", "name", "Cancha Actualizada", "location", "Zona A",
                "price", 99.0, "status", "mantenimiento", "description", "actualizada", "capacity", 4);

        HttpResponse<String> response = mutate(session, "PUT", "/api/canchas/" + canchaSeed.getId(), dto);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Cancha Actualizada", body.get("name").asText());
        assertEquals("mantenimiento", body.get("status").asText());
    }

    @Test
    @DisplayName("DELETE /api/canchas/{id} como ADMIN elimina la cancha (204)")
    void eliminarCanchaComoAdmin() throws Exception {
        String email = uniqueEmail("cancha-admin-delete");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        HttpResponse<String> response = mutate(session, "DELETE", "/api/canchas/" + canchaSeed.getId(), null);

        assertEquals(204, response.statusCode());
        assertFalse(canchaRepository.existsById(canchaSeed.getId()));
    }

    @Test
    @DisplayName("DELETE /api/canchas/{id} como usuario sin rol ADMIN devuelve 403")
    void eliminarCanchaComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("cancha-user-delete");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = mutate(session, "DELETE", "/api/canchas/" + canchaSeed.getId(), null);

        assertEquals(403, response.statusCode());
        assertTrue(canchaRepository.existsById(canchaSeed.getId()));
    }
}
