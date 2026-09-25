package com.sportcourt.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica empíricamente el flujo real de CSRF alrededor de /api/login,
 * tal como lo ejecuta el navegador (cookie XSRF-TOKEN + header X-XSRF-TOKEN),
 * contra un servidor embebido real (sin mocks) para pasar por el filtro de
 * seguridad de verdad. Usa java.net.http.HttpClient a propósito para no
 * depender de módulos de test de Spring Boot/Security que no están en el
 * classpath de este proyecto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfLoginFlowTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void loginSinTokenCsrfEsRechazado() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"nadie@test.com\",\"password\":\"x\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode(),
                "Sin cookie+header CSRF, Spring Security debe bloquear el POST con 403");
    }

    @Test
    void getCanchasPublicoRevisaSiDejaCookieCsrf() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/canchas"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Optional<String> setCookie = response.headers().firstValue("Set-Cookie");
        System.out.println("Set-Cookie tras GET /api/canchas (público): " + setCookie.orElse("(ninguna)"));
    }

    @Test
    void flujoCompletoCsrfTokenLuegoLoginConCredencialesInvalidas() throws Exception {
        // 1) El navegador primero debe pedir /api/csrf para obtener la cookie
        HttpRequest csrfRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/csrf"))
                .GET()
                .build();

        HttpResponse<String> csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, csrfResponse.statusCode());

        String setCookieHeader = csrfResponse.headers().firstValue("Set-Cookie")
                .orElse(null);
        assertNotNull(setCookieHeader, "GET /api/csrf debería fijar la cookie XSRF-TOKEN");
        assertTrue(setCookieHeader.contains("XSRF-TOKEN="), "La cookie debe llamarse XSRF-TOKEN");

        String rawCookie = setCookieHeader.split(";", 2)[0];
        String xsrfValue = rawCookie.substring("XSRF-TOKEN=".length());

        // 2) Con la cookie + el header X-XSRF-TOKEN, el login debería pasar el
        // filtro CSRF (y fallar por credenciales inválidas -> 401, no 403).
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/login"))
                .header("Content-Type", "application/json")
                .header("Cookie", rawCookie)
                .header("X-XSRF-TOKEN", xsrfValue)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"nadie@test.com\",\"password\":\"x\"}"))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, loginResponse.statusCode(),
                "Con el token CSRF correcto, debe pasar el filtro CSRF y fallar solo por credenciales (401)");
    }

    @Test
    void tokenDelBodyJsonCoincideConLaCookieCruda() throws Exception {
        // Regresión: con el handler por defecto (Xor) el valor expuesto en el
        // body de /api/csrf no coincide con el de la cookie, y el frontend
        // (que lee la cookie directo) nunca puede pasar el filtro CSRF.
        HttpRequest csrfRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/csrf"))
                .GET()
                .build();

        HttpResponse<String> csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
        String body = csrfResponse.body();
        String setCookieHeader = csrfResponse.headers().firstValue("Set-Cookie").orElse(null);
        assertNotNull(setCookieHeader);
        String rawCookieValue = setCookieHeader.split(";", 2)[0].substring("XSRF-TOKEN=".length());

        assertTrue(body.contains("\"token\":\"" + rawCookieValue + "\""),
                "El token expuesto en /api/csrf debe ser igual al valor crudo de la cookie XSRF-TOKEN");
    }
}
