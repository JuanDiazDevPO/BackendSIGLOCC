package com.siglocc.repository;

import com.siglocc.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Equipo}.
 *
 * <p>Utilizado en el registro de usuarios para verificar que el
 * {@code equipo_id} recibido en el request exista en la tabla {@code equipos}.</p>
 */
public interface EquipoRepository extends JpaRepository<Equipo, Integer> {

    /**
     * Retorna los IDs de todos los equipos del sistema.
     * Usado para resolver el alcance "país completo" de un usuario ENL.
     */
    @Query("SELECT e.id FROM Equipo e")
    List<Integer> findAllIds();

    /**
     * Retorna los IDs de los equipos ERL cuyo {@code erle_id} apunta al ERLE indicado.
     * Usado para resolver el alcance "mi clúster" de un usuario ERLE.
     */
    @Query("SELECT e.id FROM Equipo e WHERE e.erleId = :erleId")
    List<Integer> findIdsByErleId(@Param("erleId") Integer erleId);
}
