package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.CanchaDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Cancha;
import com.sportcourt.backend.repository.CanchaRepository;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CanchaService - Tests Unitarios")
public class CanchaServiceTest {

    @Mock
    private CanchaRepository canchaRepository;

    @InjectMocks
    private CanchaService canchaService;

    private CanchaDTO validCanchaDTO;
    private Cancha mockCancha;

    @BeforeEach
    void setUp() {
        validCanchaDTO = new CanchaDTO();
        validCanchaDTO.setSport("Fútbol");
        validCanchaDTO.setName("Cancha Principal");
        validCanchaDTO.setLocation("Zona A");
        validCanchaDTO.setPrice(50.0);
        validCanchaDTO.setCapacity(1);
        validCanchaDTO.setStatus("disponible");
        validCanchaDTO.setDescription("Cancha de fútbol con piso sintético");

        mockCancha = new Cancha();
        mockCancha.setId(1);
        mockCancha.setSport("Fútbol");
        mockCancha.setName("Cancha Principal");
        mockCancha.setLocation("Zona A");
        mockCancha.setPrice(50.0);
        mockCancha.setCapacity(1);
        mockCancha.setStatus("disponible");
        mockCancha.setDescription("Cancha de fútbol con piso sintético");
    }

    @Test
    @DisplayName("✅ Crear cancha exitosa")
    void crearCanchaExitosa() {
        when(canchaRepository.save(any(Cancha.class))).thenReturn(mockCancha);

        Cancha resultado = canchaService.crearCancha(validCanchaDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Fútbol", resultado.getSport());
        verify(canchaRepository).save(any(Cancha.class));
    }

    @Test
    @DisplayName("✅ Obtener cancha existente")
    void obtenerCanchaExistente() {
        when(canchaRepository.findById(1)).thenReturn(Optional.of(mockCancha));

        Cancha resultado = canchaService.obtenerCancha(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        verify(canchaRepository).findById(1);
    }

    @Test
    @DisplayName("❌ Obtener cancha no existente")
    void obtenerCanchaNoExistente() {
        when(canchaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            canchaService.obtenerCancha(999);
        });
    }

    @Test
    @DisplayName("✅ Listar todas las canchas")
    void listarCanchas() {
        List<Cancha> canchas = List.of(mockCancha);
        when(canchaRepository.findAll()).thenReturn(canchas);

        List<Cancha> resultado = canchaService.listarCanchas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(canchaRepository).findAll();
    }

    @Test
    @DisplayName("✅ Actualizar cancha existente")
    void actualizarCanchaExistente() {
        when(canchaRepository.findById(1)).thenReturn(Optional.of(mockCancha));
        when(canchaRepository.save(any(Cancha.class))).thenReturn(mockCancha);

        CanchaDTO updateDTO = new CanchaDTO();
        updateDTO.setName("Cancha Actualizada");
        updateDTO.setPrice(75.0);

        Cancha resultado = canchaService.actualizarCancha(1, updateDTO);

        assertNotNull(resultado);
        verify(canchaRepository).save(any(Cancha.class));
    }

    @Test
    @DisplayName("✅ Obtener capacidad de cancha")
    void obtenerCapacidadCancha() {
        when(canchaRepository.findById(1)).thenReturn(Optional.of(mockCancha));

        Integer capacidad = canchaService.obtenerCapacidadCancha(1);

        assertEquals(1, capacidad);
    }

    @Test
    @DisplayName("✅ Eliminar cancha existente")
    void eliminarCanchaExistente() {
        when(canchaRepository.findById(1)).thenReturn(Optional.of(mockCancha));
        doNothing().when(canchaRepository).deleteById(1);

        canchaService.eliminarCancha(1);

        verify(canchaRepository).deleteById(1);
    }
}
