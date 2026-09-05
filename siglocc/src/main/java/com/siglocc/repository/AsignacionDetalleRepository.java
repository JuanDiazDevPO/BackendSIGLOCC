package com.siglocc.repository;

import com.siglocc.entity.AsignacionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Repositorio de acceso a la tabla {@code asignacion_detalle}. */
public interface AsignacionDetalleRepository extends JpaRepository<AsignacionDetalle, Integer> {

    /** Lista todos los detalles de una cabecera de asignación. */
    List<AsignacionDetalle> findByCabeceraId(Integer cabeceraId);

    /** Elimina todos los detalles de una cabecera (para regenerar). */
    @Transactional
    void deleteByCabeceraId(Integer cabeceraId);

    /** Busca la línea de asignación de una iglesia para una categoría de caja específica. */
    Optional<AsignacionDetalle> findByCabeceraIdAndIglesiaIdAndCategoriaCajaId(
            Integer cabeceraId, Integer iglesiaId, Integer categoriaCajaId);

    /** Busca la línea de asignación de una iglesia para un tipo de ítem específico. */
    Optional<AsignacionDetalle> findByCabeceraIdAndIglesiaIdAndTipoItemId(
            Integer cabeceraId, Integer iglesiaId, Integer tipoItemId);

    /**
     * Suma total asignado (CONFIRMADO) de una categoría de caja para un equipo y temporada.
     * Usado para calcular el disponible en el inventario.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadAsignada), 0)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.equipoId         = :equipoId
          AND  c.temporadaId      = :temporadaId
          AND  d.categoriaCajaId  = :categoriaCajaId
          AND  c.estado           = 'CONFIRMADA'
        """)
    int sumAsignadoCajas(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId,
            @Param("categoriaCajaId") Integer categoriaCajaId);

    /**
     * Suma total asignado (CONFIRMADO) de un tipo de literatura para un equipo y temporada.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadAsignada), 0)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.equipoId    = :equipoId
          AND  c.temporadaId = :temporadaId
          AND  d.tipoItemId  = :tipoItemId
          AND  c.estado      = 'CONFIRMADA'
        """)
    int sumAsignadoLiteratura(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId,
            @Param("tipoItemId") Integer tipoItemId);

    /**
     * Suma total de cajas asignadas (corridas CONFIRMADAS) dentro del alcance visible.
     * Alimenta {@code embudoCajas.asignadas} del dashboard logístico.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadAsignada), 0)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.temporadaId = :temporadaId AND c.estado = 'CONFIRMADA'
          AND  d.categoriaCajaId IS NOT NULL
          AND  c.equipoId IN (:equipoIds)
        """)
    long sumCajasAsignadasConfirmadas(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Cuenta las iglesias distintas con al menos una línea de caja CONFIRMADA y cantidad &gt; 0.
     * Alimenta {@code embudoIglesias.conAsignacion}.
     */
    @Query("""
        SELECT COUNT(DISTINCT d.iglesiaId)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.temporadaId = :temporadaId AND c.estado = 'CONFIRMADA'
          AND  d.cantidadAsignada > 0
          AND  c.equipoId IN (:equipoIds)
        """)
    long countIglesiasConAsignacionConfirmada(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Suma de cajas asignadas CONFIRMADAS agrupada por categoría, dentro del alcance visible.
     * Cada fila del resultado es {@code [categoriaCajaId (Integer), total (Long)]}.
     */
    @Query("""
        SELECT d.categoriaCajaId, COALESCE(SUM(d.cantidadAsignada), 0)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.temporadaId = :temporadaId AND c.estado = 'CONFIRMADA'
          AND  d.categoriaCajaId IS NOT NULL
          AND  c.equipoId IN (:equipoIds)
        GROUP BY d.categoriaCajaId
        """)
    List<Object[]> sumCajasAsignadasPorCategoria(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /** Suma de cajas asignadas CONFIRMADAS de un solo equipo (avance por equipo). */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadAsignada), 0)
        FROM   AsignacionDetalle  d
        JOIN   AsignacionCabecera c ON c.id = d.cabeceraId
        WHERE  c.temporadaId = :temporadaId AND c.estado = 'CONFIRMADA'
          AND  d.categoriaCajaId IS NOT NULL
          AND  c.equipoId = :equipoId
        """)
    long sumCajasAsignadasConfirmadasPorEquipo(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoId") Integer equipoId);
}
