package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.ClaseDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.repository.ClaseRepository;

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
@DisplayName("ClaseService - Tests Unitarios")
public class ClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;

    @InjectMocks
    private ClaseService claseService;

    private ClaseDTO validClaseDTO;
    private Clase mockClase;

    @BeforeEach
    void setUp() {
        validClaseDTO = new ClaseDTO();
        validClaseDTO.setName("Yoga Matutino");
        validClaseDTO.setIcon("🧘");
        validClaseDTO.setLevel("Principiante");
        validClaseDTO.setSchedule("Lunes a Viernes 7:00 AM");
        validClaseDTO.setProfessor("Juan López");
        validClaseDTO.setPrice(30.0);
        validClaseDTO.setSlots(10);

        mockClase = new Clase();
        mockClase.setId(1);
        mockClase.setName("Yoga Matutino");
        mockClase.setIcon("🧘");
        mockClase.setLevel("Principiante");
        mockClase.setSchedule("Lunes a Viernes 7:00 AM");
        mockClase.setProfessor("Juan López");
        mockClase.setPrice(30.0);
        mockClase.setSlots(10);
    }

    @Test
    @DisplayName("✅ Crear clase exitosa")
    void crearClaseExitosa() {
        when(claseRepository.save(any(Clase.class))).thenReturn(mockClase);

        Clase resultado = claseService.crearClase(validClaseDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Yoga Matutino", resultado.getName());
        verify(claseRepository).save(any(Clase.class));
    }

    @Test
    @DisplayName("✅ Obtener clase existente")
    void obtenerClaseExistente() {
        when(claseRepository.findById(1)).thenReturn(Optional.of(mockClase));

        Clase resultado = claseService.obtenerClase(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        verify(claseRepository).findById(1);
    }

    @Test
    @DisplayName("❌ Obtener clase no existente")
    void obtenerClaseNoExistente() {
        when(claseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            claseService.obtenerClase(999);
        });
    }

    @Test
    @DisplayName("✅ Listar todas las clases")
    void listarClases() {
        List<Clase> clases = List.of(mockClase);
        when(claseRepository.findAll()).thenReturn(clases);

        List<Clase> resultado = claseService.listarClases();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(claseRepository).findAll();
    }

    @Test
    @DisplayName("✅ Actualizar clase existente")
    void actualizarClaseExistente() {
        when(claseRepository.findById(1)).thenReturn(Optional.of(mockClase));
        when(claseRepository.save(any(Clase.class))).thenReturn(mockClase);

        ClaseDTO updateDTO = new ClaseDTO();
        updateDTO.setName("Yoga Vespertino");
        updateDTO.setPrice(35.0);

        Clase resultado = claseService.actualizarClase(1, updateDTO);

        assertNotNull(resultado);
        verify(claseRepository).save(any(Clase.class));
    }

    @Test
    @DisplayName("✅ Obtener cupos disponibles de clase")
    void obtenerCuposDisponibles() {
        when(claseRepository.findById(1)).thenReturn(Optional.of(mockClase));

        Integer cupos = claseService.obtenerCuposDisponibles(1);

        assertEquals(10, cupos);
    }

    @Test
    @DisplayName("✅ Eliminar clase existente")
    void eliminarClaseExistente() {
        when(claseRepository.findById(1)).thenReturn(Optional.of(mockClase));
        doNothing().when(claseRepository).deleteById(1);

        claseService.eliminarClase(1);

        verify(claseRepository).deleteById(1);
    }
}
