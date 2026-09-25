package com.sportcourt.backend.controller;

import com.sportcourt.backend.dto.CanchaDTO;
import com.sportcourt.backend.model.Cancha;
import com.sportcourt.backend.service.CanchaService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestionar canchas
 * Utiliza CanchaService para lógica de negocio
 */
@RestController
@RequestMapping("/api/canchas")
@Tag(name = "Canchas", description = "Consulta y gestión de canchas deportivas")
public class CanchaController {

    private final CanchaService canchaService;

    public CanchaController(CanchaService canchaService) {
        this.canchaService = canchaService;
    }

    /**
     * Obtener todas las canchas
     */
    @GetMapping
    public ResponseEntity<List<Cancha>> listarCanchas() {
        List<Cancha> canchas = canchaService.listarCanchas();
        return ResponseEntity.ok(canchas);
    }

    /**
     * Obtener una cancha por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cancha> buscarCancha(@PathVariable Integer id) {
        Cancha cancha = canchaService.obtenerCancha(id);
        return ResponseEntity.ok(cancha);
    }

    /**
     * Crear una nueva cancha
     */
    @PostMapping
    public ResponseEntity<Cancha> crearCancha(@Valid @RequestBody CanchaDTO canchaDTO) {
        Cancha canchaSaved = canchaService.crearCancha(canchaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(canchaSaved);
    }

    /**
     * Actualizar una cancha existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cancha> actualizarCancha(
            @PathVariable Integer id,
            @Valid @RequestBody CanchaDTO canchaDTO) {

        Cancha canchaSaved = canchaService.actualizarCancha(id, canchaDTO);
        return ResponseEntity.ok(canchaSaved);
    }

    /**
     * Eliminar una cancha
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCancha(@PathVariable Integer id) {
        canchaService.eliminarCancha(id);
        return ResponseEntity.noContent().build();
    }
}