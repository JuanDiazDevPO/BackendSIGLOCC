package com.siglocc.repository;

import com.siglocc.entity.AsignacionCabecera;
import com.siglocc.entity.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio de acceso a la tabla {@code asignacion_cabecera}. */
public interface AsignacionCabeceraRepository extends JpaRepository<AsignacionCabecera, Integer> {

    /** Lista todas las corridas de asignación de un equipo en una temporada (visión ERL). */
    List<AsignacionCabecera> findByEquipoIdAndTemporadaIdOrderByFechaGeneracionDesc(
            Integer equipoId, Integer temporadaId);

    /** Lista todas las corridas de una temporada (visión ENL). */
    List<AsignacionCabecera> findByTemporadaIdOrderByFechaGeneracionDesc(Integer temporadaId);

    /**
     * Lista las corridas del clúster de un ERLE: las del propio ERLE
     * más las de todos sus ERL subordinados.
     */
    @Query("""
        SELECT c FROM AsignacionCabecera c
        JOIN Equipo e ON e.id = c.equipoId
        WHERE c.temporadaId = :temporadaId
          AND (c.equipoId = :erleId OR e.erleId = :erleId)
        ORDER BY c.fechaGeneracion DESC
        """)
    List<AsignacionCabecera> findByErleClusterAndTemporadaOrderByFechaGeneracionDesc(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);

    /** Comprueba si ya existe una corrida en un estado dado para el equipo y temporada. */
    boolean existsByEquipoIdAndTemporadaIdAndEstado(
            Integer equipoId, Integer temporadaId, EstadoAsignacion estado);

    /** Cuenta las corridas en un estado dado dentro del alcance visible (dashboard logístico). */
    long countByTemporadaIdAndEstadoAndEquipoIdIn(
            Integer temporadaId, EstadoAsignacion estado, List<Integer> equipoIds);
}
