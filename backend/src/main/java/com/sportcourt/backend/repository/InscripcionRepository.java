package com.sportcourt.backend.repository;

import com.sportcourt.backend.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    /**
     * Obtiene las inscripciones de un usuario.
     */
    List<Inscripcion> findByUsuarioId(Integer usuarioId);

    /**
     * Indica si el usuario ya tiene una inscripción no cancelada en la clase.
     */
    @Query("""
        SELECT COUNT(i) > 0 FROM Inscripcion i
        WHERE i.usuarioId = :usuarioId
        AND i.claseId = :claseId
        AND (i.estado IS NULL OR i.estado <> 'cancelada')
        """)
    boolean existeInscripcionActiva(
            @Param("usuarioId") Integer usuarioId,
            @Param("claseId") Integer claseId);

    /**
     * Cuenta las inscripciones no canceladas de una clase.
     */
    @Query("""
        SELECT COUNT(i) FROM Inscripcion i
        WHERE i.claseId = :claseId
        AND (i.estado IS NULL OR i.estado <> 'cancelada')
        """)
    long contarInscripcionesActivas(@Param("claseId") Integer claseId);
}
