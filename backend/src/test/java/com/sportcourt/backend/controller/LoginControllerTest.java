package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración HTTP real para /api/login: rotación del ID de sesión
 * (prevención de fijación de sesión) y formato estándar de errores.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginControllerTest extends AbstractControllerTest {

    private HttpResponse<String> postLogin(Session session, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/login"))
                .header("Content-Type", "application/json")
                .header("Cookie", session.cookieHeader())
                .header("X-XSRF-TOKEN", session.xsrfValue())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String jsessionId(String cookieHeaderOrSetCookie) {
        for (String parte : cookieHeaderOrSetCookie.split(";")) {
            String p = parte.trim();
            if (p.startsWith("JSESSIONID=")) {
                return p.substring("JSESSIONID=".length());
            }
        }
        return null;
    }

    @Test
    @DisplayName("Login con una sesión ya existente cambia el ID de sesión (anti fijación de sesión)")
    void loginRotaElIdDeSesion() throws Exception {
        String email = uniqueEmail("login-rotacion");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");
        String idAnterior = jsessionId(session.cookieHeader());

        HttpResponse<String> response = postLogin(session, email, "UserPass123");

        assertEquals(200, response.statusCode(), response.body());
        String idNuevo = response.headers().allValues("Set-Cookie").stream()
                .map(this::jsessionId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);

        assertTrue(idNuevo != null, "El login debe emitir un JSESSIONID nuevo");
        assertNotEquals(idAnterior, idNuevo, "El ID de sesión previo al login no debe seguir siendo válido");

        // El ID anterior ya no está autenticado
        HttpResponse<String> conIdAnterior = get(session, "/api/reservas");
        assertEquals(403, conIdAnterior.statusCode());
    }

    @Test
    @DisplayName("Credenciales inválidas devuelven 401 con el formato estándar de ErrorResponse")
    void loginInvalidoUsaErrorResponseEstandar() throws Exception {
        String email = uniqueEmail("login-invalido");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = postLogin(session, email, "incorrecta");

        assertEquals(401, response.statusCode());
        JsonNode body = json(response);
        assertEquals(401, body.get("status").asInt());
        assertEquals("Email o contraseña incorrectos", body.get("message").asText());
        assertEquals("/api/login", body.get("path").asText());
    }
}
