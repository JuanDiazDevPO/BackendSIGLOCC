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
}
