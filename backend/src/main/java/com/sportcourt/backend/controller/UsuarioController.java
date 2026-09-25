package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestionar usuarios.
 *
 * Importante:
 * Nunca devuelve directamente la entidad Usuario,
 * porque contiene información sensible como la contraseña.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo ADMIN)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Obtener todos los usuarios sin exponer contraseñas.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {

        List<UsuarioDTO> usuarios = usuarioService.listarUsuariosDTO();

        return ResponseEntity.ok(usuarios);
    }

    /**
     * Obtener un usuario por ID sin contraseña.
     */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDTO> buscarUsuario(@PathVariable Integer id) {

        UsuarioDTO usuarioDTO = usuarioService.obtenerUsuarioDTO(id);

        return ResponseEntity.ok(usuarioDTO);
    }
}