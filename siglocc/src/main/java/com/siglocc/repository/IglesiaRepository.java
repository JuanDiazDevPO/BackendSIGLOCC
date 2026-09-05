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

    /** Cuenta las iglesias inscritas (cualquier estado) en el alcance visible. Usado por el dashboard logístico. */
    long countByTemporadaIdAndEquipoIdIn(Integer temporadaId, List<Integer> equipoIds);

    /** Cuenta las iglesias en un estado dado dentro del alcance visible. Usado por el dashboard logístico. */
    long countByTemporadaIdAndEstadoAndEquipoIdIn(
            Integer temporadaId, EstadoIglesia estado, List<Integer> equipoIds);

    /** Cuenta las iglesias de un equipo en un estado dado (avance por equipo del dashboard logístico). */
    long countByEquipoIdAndTemporadaIdAndEstado(
            Integer equipoId, Integer temporadaId, EstadoIglesia estado);

    /**
     * Suma las cajas solicitadas por las iglesias APROBADAS dentro del alcance visible.
     * Base del embudo de cajas ({@code embudoCajas.solicitadas}).
     */
    @Query("""
        SELECT COALESCE(SUM(i.cajasSolicitadas), 0) FROM Iglesia i
        WHERE i.temporadaId = :temporadaId AND i.estado = 'APROBADA'
          AND i.equipoId IN (:equipoIds)
        """)
    long sumCajasSolicitadasAprobadas(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Cuenta las iglesias APROBADAS que todavía no tienen registro de capacitación.
     * Alimenta {@code pendientes.iglesiasAprobadasSinCapacitar}.
     */
    @Query("""
        SELECT COUNT(i) FROM Iglesia i
        WHERE i.temporadaId = :temporadaId AND i.estado = 'APROBADA'
          AND i.equipoId IN (:equipoIds)
          AND NOT EXISTS (
              SELECT 1 FROM CapacitacionIglesia c
              WHERE c.iglesiaId = i.id AND c.temporadaId = i.temporadaId
          )
        """)
    long countAprobadasSinCapacitar(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);
}
