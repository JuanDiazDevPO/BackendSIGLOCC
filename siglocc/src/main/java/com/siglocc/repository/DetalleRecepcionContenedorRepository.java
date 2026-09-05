package com.siglocc.repository;

import com.siglocc.entity.DetalleRecepcionContenedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code detalle_recepcion_contenedor}. */
public interface DetalleRecepcionContenedorRepository
        extends JpaRepository<DetalleRecepcionContenedor, Integer> {

    /** Lista todos los detalles de una recepción. */
    List<DetalleRecepcionContenedor> findByRecepcionId(Integer recepcionId);

    /** Elimina todos los detalles de una recepción. */
    @Transactional
    void deleteByRecepcionId(Integer recepcionId);

    /**
     * Suma el stock recibido de una categoría de caja para un equipo y temporada.
     * Suma todas las recepciones del equipo en esa temporada.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidad), 0)
        FROM   DetalleRecepcionContenedor d
        JOIN   RecepcionContenedor r ON r.id = d.recepcionId
        WHERE  r.equipoId        = :equipoId
          AND  r.temporadaId     = :temporadaId
          AND  d.categoriaCajaId = :categoriaCajaId
        """)
    int sumCajasByEquipoAndTemporadaAndCategoria(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId,
            @Param("categoriaCajaId") Integer categoriaCajaId);

    /**
     * Suma el stock recibido de un tipo de literatura para un equipo y temporada.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidad), 0)
        FROM   DetalleRecepcionContenedor d
        JOIN   RecepcionContenedor r ON r.id = d.recepcionId
        WHERE  r.equipoId    = :equipoId
          AND  r.temporadaId = :temporadaId
          AND  d.tipoItemId  = :tipoItemId
        """)
    int sumLiteraturaByEquipoAndTemporadaAndTipo(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId,
            @Param("tipoItemId") Integer tipoItemId);

    /**
     * Suma total de cajas recibidas dentro del alcance visible.
     * Alimenta {@code embudoCajas.recibidas} del dashboard logístico.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidad), 0)
        FROM   DetalleRecepcionContenedor d
        JOIN   RecepcionContenedor r ON r.id = d.recepcionId
        WHERE  r.temporadaId = :temporadaId
          AND  d.categoriaCajaId IS NOT NULL
          AND  r.equipoId IN (:equipoIds)
        """)
    long sumCajasRecibidas(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Suma de cajas recibidas agrupada por categoría, dentro del alcance visible.
     * Cada fila del resultado es {@code [categoriaCajaId (Integer), total (Long)]}.
     */
    @Query("""
        SELECT d.categoriaCajaId, COALESCE(SUM(d.cantidad), 0)
        FROM   DetalleRecepcionContenedor d
        JOIN   RecepcionContenedor r ON r.id = d.recepcionId
        WHERE  r.temporadaId = :temporadaId
          AND  d.categoriaCajaId IS NOT NULL
          AND  r.equipoId IN (:equipoIds)
        GROUP BY d.categoriaCajaId
        """)
    List<Object[]> sumCajasRecibidasPorCategoria(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Suma de literatura recibida agrupada por tipo de ítem, dentro del alcance visible.
     * Cada fila del resultado es {@code [tipoItemId (Integer), total (Long)]}.
     */
    @Query("""
        SELECT d.tipoItemId, COALESCE(SUM(d.cantidad), 0)
        FROM   DetalleRecepcionContenedor d
        JOIN   RecepcionContenedor r ON r.id = d.recepcionId
        WHERE  r.temporadaId = :temporadaId
          AND  d.tipoItemId IS NOT NULL
          AND  r.equipoId IN (:equipoIds)
        GROUP BY d.tipoItemId
        """)
    List<Object[]> sumLiteraturaRecibidaPorTipo(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);
}
