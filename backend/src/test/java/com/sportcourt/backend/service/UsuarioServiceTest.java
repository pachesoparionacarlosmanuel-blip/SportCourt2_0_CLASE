package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.UsuarioDTO;
import com.sportcourt.backend.exception.BusinessException;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Usuario;
import com.sportcourt.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Tests Unitarios")
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

    // =================================================
    // Formato de email según el rol
    // =================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "victor151994alianza@gmail.com",
            "Milagros06alejo@gmail.com",
            "carlospacheco200319@gmail.com",
            "victor.xc@hotmail.com",
            "carlos_250198@hotmail.com"
    })
    @DisplayName("✅ Email válido para rol usuario (gmail/hotmail)")
    void emailValidoParaUsuario(String email) {
        assertDoesNotThrow(() -> usuarioService.validarEmailSegunRol(email, "usuario"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "victor151994@yahoo.com",
            "victor.xc@outlook.com",
            "CarlosPacheco_Administrador@sportcourt.com.pe",
            "carlos@gmail.com.pe",
            "carlos@hotmail.es",
            "juan@example.com"
    })
    @DisplayName("❌ Email inválido para rol usuario")
    void emailInvalidoParaUsuario(String email) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.validarEmailSegunRol(email, "usuario"));
        assertTrue(ex.getMessage().contains("@gmail.com o @hotmail.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CarlosPacheco_Administrador@sportcourt.com.pe",
            "VictorPacheco_Administrador@sportcourt.com.pe",
            "BrissaTaquiri_Administrador@sportcourt.com.pe",
            "MariaTaquri_Administrador@sportcourt.com.pe"
    })
    @DisplayName("✅ Email válido para rol admin")
    void emailValidoParaAdmin(String email) {
        assertDoesNotThrow(() -> usuarioService.validarEmailSegunRol(email, "admin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CarlosPacheco@gmail.com",
            "carlos_250198@hotmail.com",
            "CarlosPacheco@sportcourt.com.pe",
            "Carlos_Pacheco_Administrador@sportcourt.com.pe",
            "carlospacheco_Administrador@sportcourt.com.pe",
            "Carlos_Administrador@sportcourt.com.pe",
            "CarlosPacheco_administrador@sportcourt.com.pe",
            "CarlosPacheco_Administrador@sportcourt.com",
            "CarlosPacheco_Administrador@gmail.com"
    })
    @DisplayName("❌ Email inválido para rol admin")
    void emailInvalidoParaAdmin(String email) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.validarEmailSegunRol(email, "admin"));
        assertTrue(ex.getMessage().contains("_Administrador@sportcourt.com.pe"));
    }

    @Test
    @DisplayName("❌ Rol desconocido es rechazado")
    void rolDesconocidoRechazado() {
        assertThrows(BusinessException.class,
                () -> usuarioService.validarEmailSegunRol("victor.xc@hotmail.com", "superusuario"));
    }

    // =================================================
    // Crear usuario
    // =================================================

    @Test
    @DisplayName("✅ Crear usuario con Gmail guarda la contraseña encriptada")
    void crearUsuarioValido() {
        UsuarioDTO dto = new UsuarioDTO(null, "Victor", "victor151994alianza@gmail.com", "usuario");
        dto.setPassword("Clave123");
        when(usuarioRepository.findByEmail("victor151994alianza@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10);
            return u;
        });

        UsuarioDTO resultado = usuarioService.crearUsuario(dto);

        assertEquals(10, resultado.getId());
        assertEquals("usuario", resultado.getRol());
        assertNull(resultado.getPassword());
        verify(usuarioRepository).save(argThat(u -> "hash".equals(u.getPassword())));
    }

    @Test
    @DisplayName("✅ Crear administrador con formato _Administrador@sportcourt.com.pe")
    void crearAdminValido() {
        UsuarioDTO dto = new UsuarioDTO(null, "Carlos Pacheco",
                "CarlosPacheco_Administrador@sportcourt.com.pe", "admin");
        dto.setPassword("Admin123");
        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.crearUsuario(dto);

        assertEquals("admin", resultado.getRol());
        assertEquals("CarlosPacheco_Administrador@sportcourt.com.pe", resultado.getEmail());
    }

    @Test
    @DisplayName("❌ Crear administrador con Gmail es rechazado y no se guarda")
    void crearAdminConGmailRechazado() {
        UsuarioDTO dto = new UsuarioDTO(null, "Carlos", "carlospacheco200319@gmail.com", "admin");
        dto.setPassword("Admin123");

        assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Crear usuario con email duplicado es rechazado")
    void crearUsuarioEmailDuplicado() {
        UsuarioDTO dto = new UsuarioDTO(null, "Victor", "victor.xc@hotmail.com", "usuario");
        dto.setPassword("Clave123");
        when(usuarioRepository.findByEmail("victor.xc@hotmail.com")).thenReturn(Optional.of(mockUsuario));

        BusinessException ex = assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(dto));
        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Crear usuario sin contraseña es rechazado")
    void crearUsuarioSinPassword() {
        UsuarioDTO dto = new UsuarioDTO(null, "Victor", "victor.xc@hotmail.com", "usuario");
        when(usuarioRepository.findByEmail("victor.xc@hotmail.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(dto));
        verify(usuarioRepository, never()).save(any());
    }

    // =================================================
    // Modificar usuario
    // =================================================

    @Test
    @DisplayName("❌ Cambiar rol a admin manteniendo correo Gmail es rechazado")
    void actualizarRolAAdminConGmailRechazado() {
        mockUsuario.setEmail("victor151994alianza@gmail.com");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));
        UsuarioDTO dto = new UsuarioDTO(1, "Juan Pérez", "victor151994alianza@gmail.com", "admin");

        assertThrows(BusinessException.class, () -> usuarioService.actualizarUsuario(1, dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Administrador no puede cambiar su correo a Hotmail")
    void actualizarAdminConHotmailRechazado() {
        mockUsuario.setEmail("CarlosPacheco_Administrador@sportcourt.com.pe");
        mockUsuario.setRol("admin");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));
        UsuarioDTO dto = new UsuarioDTO(1, "Carlos Pacheco", "carlos_250198@hotmail.com", "admin");

        assertThrows(BusinessException.class, () -> usuarioService.actualizarUsuario(1, dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Modificar email a uno que pertenece a otro usuario es rechazado")
    void actualizarEmailDuplicado() {
        Usuario otro = new Usuario();
        otro.setId(2);
        otro.setEmail("victor.xc@hotmail.com");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));
        when(usuarioRepository.findByEmail("victor.xc@hotmail.com")).thenReturn(Optional.of(otro));
        UsuarioDTO dto = new UsuarioDTO(1, "Juan Pérez", "victor.xc@hotmail.com", "usuario");

        assertThrows(BusinessException.class, () -> usuarioService.actualizarUsuario(1, dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Modificar usuario conservando su propio email y sin cambiar contraseña")
    void actualizarUsuarioValido() {
        mockUsuario.setEmail("victor.xc@hotmail.com");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(mockUsuario));
        when(usuarioRepository.findByEmail("victor.xc@hotmail.com")).thenReturn(Optional.of(mockUsuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioDTO dto = new UsuarioDTO(1, "Victor X", "victor.xc@hotmail.com", "usuario");

        UsuarioDTO resultado = usuarioService.actualizarUsuario(1, dto);

        assertEquals("Victor X", resultado.getNombre());
        assertEquals("hashedPassword123", mockUsuario.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }
}
