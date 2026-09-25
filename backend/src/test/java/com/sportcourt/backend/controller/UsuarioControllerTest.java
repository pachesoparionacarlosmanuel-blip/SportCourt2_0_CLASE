package com.sportcourt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportcourt.backend.model.Usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración HTTP real (sin mocks) para /api/usuarios: solo ADMIN,
 * y nunca debe exponer la contraseña (SecurityConfig: hasRole("ADMIN") para
 * /api/usuarios/**).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UsuarioControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("GET /api/usuarios sin autenticar devuelve 403 (no hay AuthenticationEntryPoint configurado, no 401)")
    void listarUsuariosSinAutenticarDevuelve403() throws Exception {
        HttpResponse<String> response = get(null, "/api/usuarios");

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/usuarios como usuario sin rol ADMIN devuelve 403")
    void listarUsuariosComoUsuarioNoAdminDevuelve403() throws Exception {
        String email = uniqueEmail("usuarios-user");
        crearUsuario(email, "UserPass123", "usuario");
        Session session = login(email, "UserPass123");

        HttpResponse<String> response = get(session, "/api/usuarios");

        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/usuarios como ADMIN devuelve la lista sin exponer contraseñas")
    void listarUsuariosComoAdmin() throws Exception {
        String adminEmail = uniqueEmail("usuarios-admin");
        Usuario admin = crearUsuario(adminEmail, "AdminPass123", "admin");
        crearUsuario(uniqueEmail("usuarios-otro"), "OtroPass123", "usuario");
        Session session = login(adminEmail, "AdminPass123");

        HttpResponse<String> response = get(session, "/api/usuarios");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("password") || response.body().contains("Password123")
                || response.body().toLowerCase().contains("adminpass"),
                "La respuesta jamás debe exponer contraseñas: " + response.body());

        JsonNode body = json(response);
        assertTrue(body.isArray());
        boolean contieneAdmin = false;
        for (JsonNode nodo : body) {
            assertFalse(nodo.has("password"), "El DTO de usuario no debe tener campo password");
            if (nodo.get("id").asInt() == admin.getId()) {
                contieneAdmin = true;
                assertEquals(adminEmail, nodo.get("email").asText());
                assertEquals("admin", nodo.get("rol").asText());
            }
        }
        assertTrue(contieneAdmin);
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} como ADMIN devuelve el usuario sin contraseña")
    void buscarUsuarioComoAdmin() throws Exception {
        String adminEmail = uniqueEmail("usuarios-admin-get");
        crearUsuario(adminEmail, "AdminPass123", "admin");
        Session session = login(adminEmail, "AdminPass123");

        Usuario objetivo = crearUsuario(uniqueEmail("usuarios-objetivo"), "ObjPass123", "usuario");

        HttpResponse<String> response = get(session, "/api/usuarios/" + objetivo.getId());

        assertEquals(200, response.statusCode());
        JsonNode body = json(response);
        assertEquals(objetivo.getEmail(), body.get("email").asText());
        assertFalse(body.has("password"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} inexistente como ADMIN devuelve 404")
    void buscarUsuarioInexistenteComoAdminDevuelve404() throws Exception {
        String adminEmail = uniqueEmail("usuarios-admin-404");
        crearUsuario(adminEmail, "AdminPass123", "admin");
        Session session = login(adminEmail, "AdminPass123");

        HttpResponse<String> response = get(session, "/api/usuarios/999999");

        assertEquals(404, response.statusCode());
        JsonNode body = json(response);
        assertEquals("Not Found", body.get("error").asText());
    }
}
