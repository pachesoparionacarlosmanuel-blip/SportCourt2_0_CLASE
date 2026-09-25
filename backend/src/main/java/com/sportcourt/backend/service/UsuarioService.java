package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para usuarios
 * Maneja lógica de creación, lectura, actualización de usuarios
 */
@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtener usuario por ID
     *
     * @param id ID del usuario
     * @return Usuario encontrado
     * @throws ResourceNotFoundException si no existe
     */
    public Usuario obtenerUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado"));
    }

    /**
     * Obtener usuario por email.
     * Se utiliza para identificar al usuario autenticado.
     *
     * @param email Email del usuario autenticado
     * @return Usuario encontrado
     * @throws ResourceNotFoundException si no existe
     */
    public Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /**
     * Listar todos los usuarios
     *
     * @return Lista de usuarios
     */
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Obtener todos los usuarios como DTOs.
     * Nunca expone las contraseñas.
     */
    public List<UsuarioDTO> listarUsuariosDTO() {

        return usuarioRepository.findAll()
                .stream()
                .map(usuario -> new UsuarioDTO(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRol()))
                .toList();
    }

    /**
     * Obtener usuario como DTO (sin contraseña)
     *
     * @param id ID del usuario
     * @return UsuarioDTO sin campos sensibles
     */
    public UsuarioDTO obtenerUsuarioDTO(Integer id) {
        Usuario usuario = obtenerUsuario(id);
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol());
    }

    /**
     * Verificar que un usuario existe
     *
     * @param usuarioId ID del usuario
     * @throws ResourceNotFoundException si no existe
     */
    public void verificarUsuarioExiste(Integer usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario con ID " + usuarioId + " no encontrado");
        }
    }
}
