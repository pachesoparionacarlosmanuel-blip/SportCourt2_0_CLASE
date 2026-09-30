package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    /**
     * Crear un usuario. El formato del email se valida según el rol
     * en UsuarioService.
     */
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {

        UsuarioDTO creado = usuarioService.crearUsuario(usuarioDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Modificar un usuario existente.
     */
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioDTO usuarioDTO) {

        UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, usuarioDTO);

        return ResponseEntity.ok(actualizado);
    }
}