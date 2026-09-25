package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.LoginRequest;
import com.sportcourt.backend.dto.LoginResponse;
import com.sportcourt.backend.exception.ErrorResponse;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
@Tag(name = "Login", description = "Autenticación de usuarios")
public class LoginController {

    private final AuthService authService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Autentica un usuario y crea una sesión de Spring Security.
     */
    @PostMapping
    public ResponseEntity<?> login(
            @RequestBody LoginRequest datos,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Validar entrada
        if (datos == null ||
                datos.getEmail() == null ||
                datos.getEmail().isBlank() ||
                datos.getPassword() == null ||
                datos.getPassword().isBlank()) {

            return error(HttpStatus.BAD_REQUEST, "Email y contraseña son requeridos", request);
        }

        // Autenticar usuario mediante BCrypt
        Usuario usuario = authService.authenticate(
                datos.getEmail(),
                datos.getPassword()
        );

        // Credenciales incorrectas
        if (usuario == null) {
            return error(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos", request);
        }

        // Crear autoridad según el rol del usuario
        String rol = usuario.getRol();

        if (rol == null || rol.isBlank()) {
            rol = "usuario";
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario.getEmail(),
                        null,
                        java.util.List.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_" + rol.toUpperCase()
                                )
                        )
                );

        // Crear contexto de seguridad
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        // Evitar fijación de sesión: si ya existía una sesión antes del login
        // (creada siendo anónimo), se le asigna un ID nuevo para que un ID
        // conocido de antemano no quede autenticado.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        // Guardar autenticación en la sesión HTTP
        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        // Retornar datos del usuario sin contraseña
        LoginResponse loginResponse = new LoginResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );

        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Respuesta de error con el mismo formato que GlobalExceptionHandler.
     */
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String mensaje, HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                        status.value(),
                        status.getReasonPhrase(),
                        mensaje,
                        request.getRequestURI()));
    }
}
