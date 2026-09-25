package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.ClaseDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Clase;
import com.sportcourt.backend.repository.ClaseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para clases
 * Maneja lógica de creación, lectura, actualización de clases
 */
@Service
@Transactional
public class ClaseService {

    private final ClaseRepository claseRepository;

    public ClaseService(ClaseRepository claseRepository) {
        this.claseRepository = claseRepository;
    }

    /**
     * Obtener clase por ID
     * @param id ID de la clase
     * @return Clase encontrada
     * @throws ResourceNotFoundException si no existe
     */
    public Clase obtenerClase(Integer id) {
        return claseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clase con ID " + id + " no encontrada"));
    }

    /**
     * Bloquea la fila de la clase hasta el fin de la transacción actual
     * (SELECT ... FOR UPDATE), para que dos operaciones simultáneas no
     * superen su capacidad validando ambas con el mismo conteo.
     * @param id ID de la clase
     * @throws ResourceNotFoundException si no existe
     */
    public void bloquearClase(Integer id) {
        claseRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clase con ID " + id + " no encontrada"));
    }

    /**
     * Listar todas las clases
     * @return Lista de clases
     */
    public List<Clase> listarClases() {
        return claseRepository.findAll();
    }

    /**
     * Crear una nueva clase
     * @param claseDTO DTO con datos de la clase
     * @return Clase creada
     */
    public Clase crearClase(ClaseDTO claseDTO) {
        Clase clase = new Clase();
        clase.setName(claseDTO.getName());
        clase.setIcon(claseDTO.getIcon());
        clase.setLevel(claseDTO.getLevel());
        clase.setSchedule(claseDTO.getSchedule());
        clase.setProfessor(claseDTO.getProfessor());
        clase.setPrice(claseDTO.getPrice());
        clase.setSlots(claseDTO.getSlots());

        return claseRepository.save(clase);
    }

    /**
     * Actualizar una clase existente
     * @param id ID de la clase
     * @param claseDTO DTO con datos nuevos
     * @return Clase actualizada
     * @throws ResourceNotFoundException si no existe
     */
    public Clase actualizarClase(Integer id, ClaseDTO claseDTO) {
        Clase clase = obtenerClase(id);

        clase.setName(claseDTO.getName());
        clase.setIcon(claseDTO.getIcon());
        clase.setLevel(claseDTO.getLevel());
        clase.setSchedule(claseDTO.getSchedule());
        clase.setProfessor(claseDTO.getProfessor());
        clase.setPrice(claseDTO.getPrice());
        clase.setSlots(claseDTO.getSlots());

        return claseRepository.save(clase);
    }

    /**
     * Eliminar una clase
     * @param id ID de la clase
     * @throws ResourceNotFoundException si no existe
     */
    public void eliminarClase(Integer id) {
        Clase clase = obtenerClase(id);
        claseRepository.deleteById(id);
    }

    /**
     * Obtener cupos disponibles en una clase
     * @param claseId ID de la clase
     * @return Cupos disponibles
     * @throws ResourceNotFoundException si no existe
     */
    public Integer obtenerCuposDisponibles(Integer claseId) {
        Clase clase = obtenerClase(claseId);
        return clase.getSlots();
    }
}
