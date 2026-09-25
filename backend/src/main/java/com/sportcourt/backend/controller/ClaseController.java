package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.ClaseDTO;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.service.ClaseService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestionar clases
 * Utiliza ClaseService para lógica de negocio
 */
@RestController
@RequestMapping("/api/clases")
@Tag(name = "Clases", description = "Consulta y gestión de clases deportivas")
public class ClaseController {

    private final ClaseService claseService;

    public ClaseController(ClaseService claseService) {
        this.claseService = claseService;
    }

    /**
     * Obtener todas las clases
     */
    @GetMapping
    public ResponseEntity<List<Clase>> listarClases() {
        List<Clase> clases = claseService.listarClases();
        return ResponseEntity.ok(clases);
    }

    /**
     * Obtener una clase por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Clase> buscarClase(@PathVariable Integer id) {
        Clase clase = claseService.obtenerClase(id);
        return ResponseEntity.ok(clase);
    }

    /**
     * Crear una nueva clase
     */
    @PostMapping
    public ResponseEntity<Clase> crearClase(@Valid @RequestBody ClaseDTO claseDTO) {
        Clase claseSaved = claseService.crearClase(claseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(claseSaved);
    }

    /**
     * Actualizar una clase existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Clase> actualizarClase(
            @PathVariable Integer id,
            @Valid @RequestBody ClaseDTO claseDTO) {

        Clase claseSaved = claseService.actualizarClase(id, claseDTO);
        return ResponseEntity.ok(claseSaved);
    }

    /**
     * Eliminar una clase
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarClase(@PathVariable Integer id) {
        claseService.eliminarClase(id);
        return ResponseEntity.noContent().build();
    }
}