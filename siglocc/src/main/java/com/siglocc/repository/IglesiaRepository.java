package com.siglocc.repository;

import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio de acceso a la tabla {@code iglesias}. */
public interface IglesiaRepository extends JpaRepository<Iglesia, Integer> {

    /** Lista todas las iglesias de una temporada (visión ENL). */
    List<Iglesia> findByTemporadaId(Integer temporadaId);

    /** Lista las iglesias de un equipo ERL específico en una temporada. */
    List<Iglesia> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);

    /**
     * Lista iglesias del clúster de un ERLE: las del propio ERLE
     * más las de todos sus ERL subordinados.
     */
    @Query("""
        SELECT i FROM Iglesia i
        JOIN Equipo e ON e.id = i.equipoId
        WHERE i.temporadaId = :temporadaId
          AND (i.equipoId = :erleId OR e.erleId = :erleId)
        """)
    List<Iglesia> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);

    /**
     * Lista las iglesias de un equipo en una temporada filtradas por estado.
     * Usado por el motor de asignación para obtener solo las iglesias APROBADAS.
     */
    List<Iglesia> findByEquipoIdAndTemporadaIdAndEstado(
            Integer equipoId, Integer temporadaId, EstadoIglesia estado);

    /**
     * Verifica si ya existe una iglesia con ese nombre para el mismo equipo y temporada.
     * Previene duplicados antes de llegar a la restricción UNIQUE de la BD.
     */
    boolean existsByNombreAndEquipoIdAndTemporadaId(
            String nombre, Integer equipoId, Integer temporadaId);
}
