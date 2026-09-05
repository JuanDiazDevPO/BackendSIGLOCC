package com.siglocc.repository;

import com.siglocc.entity.CapacitacionIglesia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorio de acceso a la tabla {@code capacitaciones_iglesia}. */
public interface CapacitacionIglesiaRepository extends JpaRepository<CapacitacionIglesia, Integer> {

    /** Busca la capacitación de una iglesia en una temporada específica. */
    Optional<CapacitacionIglesia> findByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);

    /** Verifica si ya existe registro de capacitación para esa iglesia y temporada. */
    boolean existsByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);

    /**
     * Lista todas las capacitaciones de una temporada cuyo equipo de iglesia
     * pertenece al clúster de un ERLE.
     */
    @Query("""
        SELECT c FROM CapacitacionIglesia c
        JOIN Iglesia i ON i.id = c.iglesiaId
        JOIN Equipo e ON e.id = i.equipoId
        WHERE c.temporadaId = :temporadaId
          AND (i.equipoId = :erleId OR e.erleId = :erleId)
        """)
    List<CapacitacionIglesia> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);

    /** Lista todas las capacitaciones de una temporada (visión ENL). */
    List<CapacitacionIglesia> findByTemporadaId(Integer temporadaId);

    /**
     * Lista las capacitaciones de iglesias que pertenecen a un equipo ERL específico.
     */
    @Query("""
        SELECT c FROM CapacitacionIglesia c
        JOIN Iglesia i ON i.id = c.iglesiaId
        WHERE c.temporadaId = :temporadaId AND i.equipoId = :equipoId
        """)
    List<CapacitacionIglesia> findByEquipoAndTemporada(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId);

    /**
     * Cuenta las iglesias distintas con capacitación registrada dentro del alcance visible.
     * Usado por el dashboard logístico ({@code embudoIglesias.capacitadas}) y para
     * determinar si el momento actual ya llegó a Capacitación.
     */
    @Query("""
        SELECT COUNT(DISTINCT c.iglesiaId) FROM CapacitacionIglesia c
        JOIN Iglesia i ON i.id = c.iglesiaId
        WHERE c.temporadaId = :temporadaId AND i.equipoId IN (:equipoIds)
        """)
    long countDistinctIglesiaByTemporadaAndEquipos(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /** Cuenta las capacitaciones de las iglesias de un equipo específico (avance por equipo). */
    @Query("""
        SELECT COUNT(DISTINCT c.iglesiaId) FROM CapacitacionIglesia c
        JOIN Iglesia i ON i.id = c.iglesiaId
        WHERE c.temporadaId = :temporadaId AND i.equipoId = :equipoId
        """)
    long countDistinctIglesiaByTemporadaAndEquipo(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoId") Integer equipoId);
}
