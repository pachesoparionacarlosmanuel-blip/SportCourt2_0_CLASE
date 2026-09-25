package com.sportcourt.backend.repository;

import com.sportcourt.backend.model.Clase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Integer> {

    /**
     * Lee la fila con SELECT ... FOR UPDATE para serializar, dentro de la
     * transacción, las operaciones concurrentes que validan su capacidad.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT x FROM Clase x WHERE x.id = :id")
    Optional<Clase> findByIdParaActualizar(@Param("id") Integer id);
}
