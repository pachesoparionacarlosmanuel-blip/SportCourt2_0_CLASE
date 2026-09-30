package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de negocio para usuarios
 * Maneja lógica de creación, lectura, actualización de usuarios
 */
@Service
@Transactional
public class UsuarioService {

    public static final String ROL_USUARIO = "usuario";
    public static final String ROL_ADMIN = "admin";

    // Usuario: cualquier correo @gmail.com o @hotmail.com
    private static final Pattern EMAIL_USUARIO = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(gmail|hotmail)\\.com$", Pattern.CASE_INSENSITIVE);

    // Admin: PrimerNombrePrimerApellido_Administrador@sportcourt.com.pe
    // (dos palabras capitalizadas pegadas, sin separadores)
    private static final Pattern EMAIL_ADMIN = Pattern.compile(
            "^[A-Z][a-z]+[A-Z][a-z]+_Administrador@sportcourt\\.com\\.pe$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida que el email tenga el formato permitido para el rol que se
     * está guardando.
     *
     * @throws BusinessException si el rol no existe o el email no cumple
     */
    public void validarEmailSegunRol(String email, String rol) {
        String emailLimpio = email == null ? "" : email.trim();

        if (ROL_USUARIO.equals(rol)) {
            if (!EMAIL_USUARIO.matcher(emailLimpio).matches()) {
                throw new BusinessException(
                        "El correo de un usuario debe terminar en @gmail.com o @hotmail.com");
            }
        } else if (ROL_ADMIN.equals(rol)) {
            if (!EMAIL_ADMIN.matcher(emailLimpio).matches()) {
                throw new BusinessException(
                        "El correo de un administrador debe tener el formato "
                                + "PrimerNombrePrimerApellido_Administrador@sportcourt.com.pe "
                                + "(ejemplo: CarlosPacheco_Administrador@sportcourt.com.pe)");
            }
        } else {
            throw new BusinessException("Rol no válido. Valores permitidos: usuario, admin");
        }
    }

    /**
     * Crear un usuario nuevo.
     * Valida formato del email según el rol, email duplicado y contraseña.
     */
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        String rol = normalizarRol(dto.getRol());
        String email = dto.getEmail().trim();

        validarEmailSegunRol(email, rol);

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya existe un usuario registrado con el email " + email);
        }

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("La contraseña es requerida");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre().trim());
        usuario.setEmail(email);
        usuario.setRol(rol);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        return aDTO(usuarioRepository.save(usuario));
    }

    /**
     * Modificar un usuario existente.
     * La regla del email se aplica según el rol que se va a guardar, así que
     * cambiar de rol sin cambiar el correo (o viceversa) también se valida.
     * La contraseña solo se cambia si viene informada.
     */
    public UsuarioDTO actualizarUsuario(Integer id, UsuarioDTO dto) {
        Usuario usuario = obtenerUsuario(id);

        String rol = normalizarRol(dto.getRol());
        String email = dto.getEmail().trim();

        validarEmailSegunRol(email, rol);

        usuarioRepository.findByEmail(email)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessException("Ya existe un usuario registrado con el email " + email);
                });

        usuario.setNombre(dto.getNombre().trim());
        usuario.setEmail(email);
        usuario.setRol(rol);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return aDTO(usuarioRepository.save(usuario));
    }

    private String normalizarRol(String rol) {
        return rol == null ? "" : rol.trim().toLowerCase();
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol());
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
