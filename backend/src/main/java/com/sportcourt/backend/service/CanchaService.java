package com.sportcourt.backend.service;

import com.sportcourt.backend.dto.CanchaDTO;
import com.sportcourt.backend.exception.ResourceNotFoundException;
import com.sportcourt.backend.model.Cancha;
import com.sportcourt.backend.repository.CanchaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para canchas
 * Maneja lógica de creación, lectura, actualización de canchas
 */
@Service
@Transactional
public class CanchaService {

    private final CanchaRepository canchaRepository;

    public CanchaService(CanchaRepository canchaRepository) {
        this.canchaRepository = canchaRepository;
    }

    /**
     * Obtener cancha por ID
     * @param id ID de la cancha
     * @return Cancha encontrada
     * @throws ResourceNotFoundException si no existe
     */
    public Cancha obtenerCancha(Integer id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha con ID " + id + " no encontrada"));
    }

    /**
     * Bloquea la fila de la cancha hasta el fin de la transacción actual
     * (SELECT ... FOR UPDATE), para que dos operaciones simultáneas no
     * superen su capacidad validando ambas con el mismo conteo.
     * @param id ID de la cancha
     * @throws ResourceNotFoundException si no existe
     */
    public void bloquearCancha(Integer id) {
        canchaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha con ID " + id + " no encontrada"));
    }

    /**
     * Listar todas las canchas
     * @return Lista de canchas
     */
    public List<Cancha> listarCanchas() {
        return canchaRepository.findAll();
    }

    /**
     * Crear una nueva cancha
     * @param canchaDTO DTO con datos de la cancha
     * @return Cancha creada
     */
    public Cancha crearCancha(CanchaDTO canchaDTO) {
        Cancha cancha = new Cancha();
        cancha.setSport(canchaDTO.getSport());
        cancha.setName(canchaDTO.getName());
        cancha.setDescription(canchaDTO.getDescription());
        cancha.setLocation(canchaDTO.getLocation());
        cancha.setPrice(canchaDTO.getPrice());
        cancha.setStatus(canchaDTO.getStatus());
        cancha.setCapacity(canchaDTO.getCapacity());
        cancha.setImage(canchaDTO.getImage());

        return canchaRepository.save(cancha);
    }

    /**
     * Actualizar una cancha existente
     * @param id ID de la cancha
     * @param canchaDTO DTO con datos nuevos
     * @return Cancha actualizada
     * @throws ResourceNotFoundException si no existe
     */
    public Cancha actualizarCancha(Integer id, CanchaDTO canchaDTO) {
        Cancha cancha = obtenerCancha(id);

        cancha.setSport(canchaDTO.getSport());
        cancha.setName(canchaDTO.getName());
        cancha.setDescription(canchaDTO.getDescription());
        cancha.setLocation(canchaDTO.getLocation());
        cancha.setPrice(canchaDTO.getPrice());
        cancha.setStatus(canchaDTO.getStatus());
        cancha.setCapacity(canchaDTO.getCapacity());
        cancha.setImage(canchaDTO.getImage());

        return canchaRepository.save(cancha);
    }

    /**
     * Eliminar una cancha
     * @param id ID de la cancha
     * @throws ResourceNotFoundException si no existe
     */
    public void eliminarCancha(Integer id) {
        Cancha cancha = obtenerCancha(id);
        canchaRepository.deleteById(id);
    }

    /**
     * Verificar que una cancha existe y obtener su capacidad
     * @param canchaId ID de la cancha
     * @return Capacidad de la cancha
     * @throws ResourceNotFoundException si no existe
     */
    public Integer obtenerCapacidadCancha(Integer canchaId) {
        Cancha cancha = obtenerCancha(canchaId);
        return cancha.getCapacity();
    }
}
