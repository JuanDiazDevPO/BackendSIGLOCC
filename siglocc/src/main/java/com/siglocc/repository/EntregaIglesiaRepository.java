package com.siglocc.repository;

import com.siglocc.entity.EntregaIglesia;
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
}
