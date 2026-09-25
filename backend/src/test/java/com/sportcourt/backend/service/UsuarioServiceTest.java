package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Tests Unitarios")
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario mockUsuario;

    @BeforeEach
    void setUp() {
        mockUsuario = new Usuario();
        mockUsuario.setId(1);
        mockUsuario.setNombre("Juan Pérez");
        mockUsuario.setEmail("juan@example.com");
        mockUsuario.setPassword("hashedPassword123");
        mockUsuario.setRol("usuario");
    }

    @Test
    @DisplayName("✅ Obtener usuario existente")
    void obtenerUsuarioExistente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));

        Usuario resultado = usuarioService.obtenerUsuario(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Juan Pérez", resultado.getNombre());
        verify(usuarioRepository).findById(1);
    }

    @Test
    @DisplayName("❌ Obtener usuario no existente")
    void obtenerUsuarioNoExistente() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            usuarioService.obtenerUsuario(999);
        });
    }

    @Test
    @DisplayName("✅ Listar todos los usuarios")
    void listarUsuarios() {
        List<Usuario> usuarios = List.of(mockUsuario);
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        List<Usuario> resultado = usuarioService.listarUsuarios();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(usuarioRepository).findAll();
    }

    @Test
    @DisplayName("✅ Obtener usuario como DTO (sin password)")
    void obtenerUsuarioDTOSinPassword() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));

        UsuarioDTO resultado = usuarioService.obtenerUsuarioDTO(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Juan Pérez", resultado.getNombre());
        assertEquals("juan@example.com", resultado.getEmail());
        assertEquals("usuario", resultado.getRol());
        verify(usuarioRepository).findById(1);
    }

    @Test
    @DisplayName("✅ Verificar usuario existe")
    void verificarUsuarioExiste() {
        when(usuarioRepository.existsById(1)).thenReturn(true);

        // No debe lanzar excepción
        usuarioService.verificarUsuarioExiste(1);
        verify(usuarioRepository).existsById(1);
    }

    @Test
    @DisplayName("❌ Verificar usuario no existe - lanza ResourceNotFoundException")
    void verificarUsuarioNoExiste() {
        when(usuarioRepository.existsById(999)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            usuarioService.verificarUsuarioExiste(999);
        });
        verify(usuarioRepository).existsById(999);
    }
}
