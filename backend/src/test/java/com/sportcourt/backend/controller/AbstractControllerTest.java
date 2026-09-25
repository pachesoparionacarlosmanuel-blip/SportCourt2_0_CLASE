package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Infraestructura común para los tests de integración HTTP de los
 * controllers. Sigue el mismo patrón que CsrfLoginFlowTest: servidor
 * embebido real (RANDOM_PORT), perfil "test" (H2), sin mocks y sin el
 * módulo spring-security-test (no está en el classpath del proyecto).
 *
 * Cada subclase concreta debe llevar sus propias anotaciones
 * {@code @SpringBootTest(webEnvironment = RANDOM_PORT)}, {@code @ActiveProfiles("test")}
 * y {@code @DirtiesContext(classMode = ClassMode.AFTER_CLASS)} (esta última
 * para evitar que clases distintas reusen el mismo contexto/H2 en memoria y
 * se contaminen datos entre clases, ya que Spring cachea el ApplicationContext
 * por firma de configuración y todas estas clases comparten la misma).
 */
abstract class AbstractControllerTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // NOTA: Spring Boot 4.1.1 en este proyecto auto-configura por defecto un
    // ObjectMapper de Jackson 3 (tools.jackson.databind.ObjectMapper) vía
    // spring-boot-starter-jackson, NO el clásico com.fasterxml.jackson.databind.ObjectMapper
    // (Jackson 2), así que no hay un bean de ese tipo para @Autowired aquí.
    // El jar clásico de Jackson 2 databind sí está en el classpath de test
    // (dependencia transitiva de springdoc), así que se instancia uno propio
    // solo para armar/leer JSON en estos tests HTTP (no necesita coincidir
    // con el serializador interno del servidor).
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final HttpClient client = HttpClient.newHttpClient();

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Sesión autenticada: cookie combinada (JSESSIONID + XSRF-TOKEN) y el
     * valor del token para el header X-XSRF-TOKEN en peticiones mutantes.
     */
    protected record Session(String cookieHeader, String xsrfValue) {
    }

    protected String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@test.com";
    }

    /**
     * Crea un usuario directamente vía el repositorio, con la contraseña
     * encriptada mediante el bean real de PasswordEncoder de la app (nunca
     * un hash BCrypt hardcodeado).
     */
    protected Usuario crearUsuario(String email, String rawPassword, String rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Test " + rol);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    /**
     * Obtiene la cookie/token CSRF pegando GET /api/csrf, tal como lo haría
     * el navegador antes de cualquier POST/PUT/DELETE.
     */
    private String[] obtenerCsrf() throws Exception {
        HttpRequest csrfRequest = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/csrf")).GET().build();
        HttpResponse<String> csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, csrfResponse.statusCode(), "GET /api/csrf debería responder 200");

        String setCookieHeader = csrfResponse.headers().firstValue("Set-Cookie").orElse(null);
        assertNotNull(setCookieHeader, "GET /api/csrf debería fijar la cookie XSRF-TOKEN");

        String rawXsrfCookie = setCookieHeader.split(";", 2)[0];
        String xsrfValue = rawXsrfCookie.substring("XSRF-TOKEN=".length());
        return new String[] { rawXsrfCookie, xsrfValue };
    }

    /**
     * Ejecuta el flujo real GET /api/csrf -> POST /api/login (con cookie +
     * header X-XSRF-TOKEN) y devuelve la sesión autenticada resultante
     * (cookie de sesión JSESSIONID + cookie/token CSRF, reutilizables en
     * llamadas posteriores).
     */
    protected Session login(String email, String rawPassword) throws Exception {
        String[] csrf = obtenerCsrf();
        String rawXsrfCookie = csrf[0];
        String xsrfValue = csrf[1];

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", rawPassword));

        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/login"))
                .header("Content-Type", "application/json")
                .header("Cookie", rawXsrfCookie)
                .header("X-XSRF-TOKEN", xsrfValue)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResponse.statusCode(),
                "Login debería responder 200 con credenciales válidas: " + loginResponse.body());

        String jsessionRaw = loginResponse.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .map(c -> c.split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El login no fijó cookie JSESSIONID"));

        String cookieHeader = jsessionRaw + "; " + rawXsrfCookie;
        return new Session(cookieHeader, xsrfValue);
    }

    /**
     * GET público/autenticado (sin CSRF, ya que las peticiones GET no lo
     * requieren). Pasa {@code session == null} para una petición anónima.
     */
    protected HttpResponse<String> get(Session session, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET();
        if (session != null) {
            builder.header("Cookie", session.cookieHeader());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * POST/PUT/DELETE con cuerpo JSON opcional. Pasa {@code session == null}
     * para una petición anónima (sin cookie ni token CSRF, tal como haría un
     * visitante no autenticado o un cliente malicioso).
     */
    protected HttpResponse<String> mutate(Session session, String method, String path, Object bodyObjectOrNull)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path));

        String jsonBody = bodyObjectOrNull == null ? "" : objectMapper.writeValueAsString(bodyObjectOrNull);
        HttpRequest.BodyPublisher publisher = bodyObjectOrNull == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);

        builder.header("Content-Type", "application/json");
        if (session != null) {
            builder.header("Cookie", session.cookieHeader());
            builder.header("X-XSRF-TOKEN", session.xsrfValue());
        }

        switch (method) {
            case "POST" -> builder.POST(publisher);
            case "PUT" -> builder.PUT(publisher);
            case "DELETE" -> builder.method("DELETE", publisher);
            default -> throw new IllegalArgumentException("Método no soportado: " + method);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }
}
