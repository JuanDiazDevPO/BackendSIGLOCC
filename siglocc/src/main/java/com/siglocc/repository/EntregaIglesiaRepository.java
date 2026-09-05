package com.siglocc.repository;

import com.siglocc.entity.EntregaIglesia;
import com.siglocc.entity.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorio de acceso a la tabla {@code entregas_iglesia}. */
public interface EntregaIglesiaRepository extends JpaRepository<EntregaIglesia, Integer> {

    /** Busca el acta de entrega de una iglesia en una temporada. */
    Optional<EntregaIglesia> findByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);

    /** Verifica si ya existe acta de entrega para esa iglesia y temporada. */
    boolean existsByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);

    /** Lista todas las entregas de una temporada (visión ENL). */
    List<EntregaIglesia> findByTemporadaId(Integer temporadaId);

    /** Lista las entregas coordinadas por un equipo específico en una temporada. */
    List<EntregaIglesia> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);

    /**
     * Lista entregas del clúster de un ERLE (propias + ERL subordinados).
     */
    @Query("""
        SELECT ent FROM EntregaIglesia ent
        JOIN Equipo e ON e.id = ent.equipoId
        WHERE ent.temporadaId = :temporadaId
          AND (ent.equipoId = :erleId OR e.erleId = :erleId)
        """)
    List<EntregaIglesia> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);

    /** Cuenta las actas de entrega dentro del alcance visible. Usado para inferir el momento actual. */
    long countByTemporadaIdAndEquipoIdIn(Integer temporadaId, List<Integer> equipoIds);

    /**
     * Cuenta las iglesias distintas con acta COMPLETADA o PARCIAL dentro del alcance visible.
     * Alimenta {@code embudoIglesias.entregadas}.
     */
    long countDistinctIglesiaIdByTemporadaIdAndEstadoInAndEquipoIdIn(
            Integer temporadaId, List<EstadoEntrega> estados, List<Integer> equipoIds);

    /** Cuenta las actas sin firma adjunta (nula o vacía) dentro del alcance visible. */
    @Query("""
        SELECT COUNT(e) FROM EntregaIglesia e
        WHERE e.temporadaId = :temporadaId
          AND (e.firmaUrl IS NULL OR e.firmaUrl = '')
          AND e.equipoId IN (:equipoIds)
        """)
    long countSinFirma(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Cuenta las actas sin ninguna foto de entrega a niños (Momento C) dentro del alcance visible.
     * {@code FotoEntregaNinos} se asocia por {@code iglesiaId + temporadaId}, no por
     * {@code entregaId} — no existe esa columna en esa tabla.
     */
    @Query("""
        SELECT COUNT(e) FROM EntregaIglesia e
        WHERE e.temporadaId = :temporadaId
          AND e.equipoId IN (:equipoIds)
          AND NOT EXISTS (
              SELECT 1 FROM FotoEntregaNinos f
              WHERE f.iglesiaId = e.iglesiaId AND f.temporadaId = e.temporadaId
          )
        """)
    long countSinFotosNinos(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);
}
