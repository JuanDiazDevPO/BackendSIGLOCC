package com.siglocc.repository;

import com.siglocc.entity.RecepcionContenedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio de acceso a la tabla {@code recepcion_contenedores}. */
public interface RecepcionContenedorRepository extends JpaRepository<RecepcionContenedor, Integer> {

    /** Lista todas las recepciones de una temporada (visión ENL). */
    List<RecepcionContenedor> findByTemporadaId(Integer temporadaId);

    /** Lista las recepciones de un equipo específico en una temporada. */
    List<RecepcionContenedor> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);

    /**
     * Lista recepciones del clúster de un ERLE.
     */
    @Query("""
        SELECT r FROM RecepcionContenedor r
        JOIN Equipo e ON e.id = r.equipoId
        WHERE r.temporadaId = :temporadaId
          AND (r.equipoId = :erleId OR e.erleId = :erleId)
        """)
    List<RecepcionContenedor> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);
}
