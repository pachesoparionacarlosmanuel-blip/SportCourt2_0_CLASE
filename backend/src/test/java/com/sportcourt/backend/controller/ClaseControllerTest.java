package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.repository.ClaseRepository;

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
 * Test de integración HTTP real (sin mocks) para /api/clases: controller ->
 * service -> repository -> H2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClaseControllerTest extends AbstractControllerTest {

    @Autowired
    private ClaseRepository claseRepository;

    private Clase claseSeed;

    @BeforeEach
    void seedClase() {
        Clase clase = new Clase();
        clase.setName("Yoga básico");
        clase.setIcon("🧘");
        clase.setLevel("principiante");
        clase.setSchedule("Lunes 18:00");
        clase.setProfessor("Ana Pérez");
        clase.setPrice(20.0);
        clase.setSlots(5);
        claseSeed = claseRepository.save(clase);
    }

    @Test
    @DisplayName("GET /api/clases es público y devuelve la lista")
    void listarClasesEsPublico() throws Exception {
        HttpResponse<String> response = get(null, "/api/clases");

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertTrue(body.isArray());
        boolean contieneSeed = false;
        for (JsonNode nodo : body) {
            if (nodo.get("id").asInt() == claseSeed.getId()) {
                contieneSeed = true;
            }
        }
        assertTrue(contieneSeed);
    }

    @Test
    @DisplayName("GET /api/clases/{id} público devuelve la clase correcta")
    void buscarClasePorIdEsPublico() throws Exception {
        HttpResponse<String> response = get(null, "/api/clases/" + claseSeed.getId());

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Yoga básico", body.get("name").asText());
        assertEquals("Ana Pérez", body.get("professor").asText());
    }

    @Test
    @DisplayName("GET /api/clases/{id} inexistente devuelve 404")
    void buscarClaseInexistenteDevuelve404() throws Exception {
        HttpResponse<String> response = get(null, "/api/clases/999999");

        assertEquals(404, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Not Found", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/clases como ADMIN crea la clase (201)")
    void crearClaseComoAdmin() throws Exception {
        String email = uniqueEmail("clase-admin");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        Map<String, Object> dto = Map.of(
                "name", "Spinning",
                "icon", "🚴",
                "level", "intermedio",
                "schedule", "Martes 19:00",
                "professor", "Carlos Ruiz",
                "price", 25.0,
                "slots", 10);

        HttpResponse<String> response = mutate(session, "POST", "/api/clases", dto);

        assertEquals(201, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Spinning", body.get("name").asText());
        assertTrue(body.get("id").asInt() > 0);
    }

    @Test
    @DisplayName("POST /api/clases con campos requeridos vacíos devuelve 400")
    void crearClaseConDatosInvalidosDevuelve400() throws Exception {
        String email = uniqueEmail("clase-admin-invalido");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        Map<String, Object> dtoInvalido = Map.of(
                "name", "",
                "icon", "",
                "level", "",
                "schedule", "",
                "professor", "",
                "price", 0,
                "slots", 0);

        HttpResponse<String> response = mutate(session, "POST", "/api/clases", dtoInvalido);

        assertEquals(400, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Validation Error", body.get("error").asText());
    }

    @Test
    @DisplayName("POST /api/clases como usuario sin rol ADMIN devuelve 403")
    void crearClaseComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("clase-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        Map<String, Object> dto = Map.of(
                "name", "X", "icon", "x", "level", "y", "schedule", "z",
                "professor", "w", "price", 5.0, "slots", 1);

        HttpResponse<String> response = mutate(session, "POST", "/api/clases", dto);

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("PUT /api/clases/{id} como ADMIN actualiza la clase")
    void actualizarClaseComoAdmin() throws Exception {
        String email = uniqueEmail("clase-admin-put");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        Map<String, Object> dto = Map.of(
                "name", "Yoga avanzado", "icon", "🧘", "level", "avanzado",
                "schedule", "Lunes 18:00", "professor", "Ana Pérez", "price", 22.0, "slots", 8);

        HttpResponse<String> response = mutate(session, "PUT", "/api/clases/" + claseSeed.getId(), dto);

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Yoga avanzado", body.get("name").asText());
        assertEquals(8, body.get("slots").asInt());
    }

    @Test
    @DisplayName("DELETE /api/clases/{id} como ADMIN elimina la clase (204)")
    void eliminarClaseComoAdmin() throws Exception {
        String email = uniqueEmail("clase-admin-delete");
        crearUsuario(email, "AdminPass123", "admin");
        Session session = login(email, "AdminPass123");

        HttpResponse<String> response = mutate(session, "DELETE", "/api/clases/" + claseSeed.getId(), null);

        assertEquals(204, response.statusCode());
        assertFalse(claseRepository.existsById(claseSeed.getId()));
    }

    @Test
    @DisplayName("DELETE /api/clases/{id} como usuario sin rol ADMIN devuelve 403")
    void eliminarClaseComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("clase-user-delete");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = mutate(session, "DELETE", "/api/clases/" + claseSeed.getId(), null);

        assertEquals(403, response.statusCode());
        assertTrue(claseRepository.existsById(claseSeed.getId()));
    }
}
