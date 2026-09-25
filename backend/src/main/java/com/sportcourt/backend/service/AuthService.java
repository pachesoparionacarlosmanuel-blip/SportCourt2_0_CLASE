package com.sportcourt.backend.service;

import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación seguro con encriptación de contraseñas
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
    /**
     * Autentica un usuario verificando email y contraseña
     * 
     * @param email    Email del usuario
     * @param password Contraseña sin encriptar
     * @return Usuario si la autenticación es exitosa, null si falla
     */
    public Usuario authenticate(String email, String password) {
        return usuarioRepository
            .findByEmail(email)
            .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()))
            .orElse(null);
    }
}
