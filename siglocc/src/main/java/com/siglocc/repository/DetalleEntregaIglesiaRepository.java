package com.siglocc.repository;

import com.siglocc.entity.DetalleEntregaIglesia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code detalle_entrega_iglesia}. */
public interface DetalleEntregaIglesiaRepository extends JpaRepository<DetalleEntregaIglesia, Integer> {

    /** Lista todos los detalles de un acta de entrega. */
    List<DetalleEntregaIglesia> findByEntregaId(Integer entregaId);

    /** Elimina todos los detalles de un acta (para reemplazar en caso de corrección). */
    @Transactional
    void deleteByEntregaId(Integer entregaId);

    /**
     * Suma total de cajas entregadas (actas COMPLETADA o PARCIAL) dentro del alcance visible.
     * Alimenta {@code embudoCajas.entregadas}.
     */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadEntregada), 0)
        FROM   DetalleEntregaIglesia d
        JOIN   EntregaIglesia e ON e.id = d.entregaId
        WHERE  e.temporadaId = :temporadaId
          AND  e.estado IN ('COMPLETADA', 'PARCIAL')
          AND  d.categoriaCajaId IS NOT NULL
          AND  e.equipoId IN (:equipoIds)
        """)
    long sumCajasEntregadas(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Suma de cajas entregadas agrupada por categoría, dentro del alcance visible.
     * Cada fila del resultado es {@code [categoriaCajaId (Integer), total (Long)]}.
     */
    @Query("""
        SELECT d.categoriaCajaId, COALESCE(SUM(d.cantidadEntregada), 0)
        FROM   DetalleEntregaIglesia d
        JOIN   EntregaIglesia e ON e.id = d.entregaId
        WHERE  e.temporadaId = :temporadaId
          AND  e.estado IN ('COMPLETADA', 'PARCIAL')
          AND  d.categoriaCajaId IS NOT NULL
          AND  e.equipoId IN (:equipoIds)
        GROUP BY d.categoriaCajaId
        """)
    List<Object[]> sumCajasEntregadasPorCategoria(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /**
     * Suma de literatura entregada agrupada por tipo de ítem, dentro del alcance visible.
     * Cada fila del resultado es {@code [tipoItemId (Integer), total (Long)]}.
     */
    @Query("""
        SELECT d.tipoItemId, COALESCE(SUM(d.cantidadEntregada), 0)
        FROM   DetalleEntregaIglesia d
        JOIN   EntregaIglesia e ON e.id = d.entregaId
        WHERE  e.temporadaId = :temporadaId
          AND  e.estado IN ('COMPLETADA', 'PARCIAL')
          AND  d.tipoItemId IS NOT NULL
          AND  e.equipoId IN (:equipoIds)
        GROUP BY d.tipoItemId
        """)
    List<Object[]> sumLiteraturaEntregadaPorTipo(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoIds") List<Integer> equipoIds);

    /** Suma de cajas entregadas de un solo equipo (avance por equipo). */
    @Query("""
        SELECT COALESCE(SUM(d.cantidadEntregada), 0)
        FROM   DetalleEntregaIglesia d
        JOIN   EntregaIglesia e ON e.id = d.entregaId
        WHERE  e.temporadaId = :temporadaId
          AND  e.estado IN ('COMPLETADA', 'PARCIAL')
          AND  d.categoriaCajaId IS NOT NULL
          AND  e.equipoId = :equipoId
        """)
    long sumCajasEntregadasPorEquipo(
            @Param("temporadaId") Integer temporadaId,
            @Param("equipoId") Integer equipoId);
}
