package com.siglocc.repository;

import com.siglocc.entity.PuntoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio de acceso a la tabla {@code puntos_entrega}. */
public interface PuntoEntregaRepository extends JpaRepository<PuntoEntrega, Integer> {

    /** Lista todos los puntos de entrega de una temporada. */
    List<PuntoEntrega> findByTemporadaId(Integer temporadaId);

    /** Lista los puntos de entrega de un equipo ERLE específico en una temporada. */
    List<PuntoEntrega> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);

    /**
     * Lista puntos de entrega del clúster de un ERLE: los propios del ERLE
     * más los de sus ERL subordinados.
     */
    @Query("""
        SELECT p FROM PuntoEntrega p
        JOIN Equipo e ON e.id = p.equipoId
        WHERE p.temporadaId = :temporadaId
          AND (p.equipoId = :erleId OR e.erleId = :erleId)
        """)
    List<PuntoEntrega> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);
}
