package com.siglocc.repository;

import com.siglocc.entity.ReporteDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla {@code reporte_detalles}.
 *
 * <p>Los detalles no se consultan de forma independiente; siempre se accede a ellos
 * en el contexto de un reporte cabezote.</p>
 */
public interface ReporteDetalleRepository extends JpaRepository<ReporteDetalle, Integer> {

    /**
     * Retorna todos los detalles de un reporte específico.
     *
     * @param reporteId ID del reporte cabezote
     * @return lista de rubros reportados, o lista vacía si el reporte no tiene detalles
     */
    List<ReporteDetalle> findByReporteId(Integer reporteId);

    /**
     * Elimina todos los detalles de un reporte. Útil si en el futuro se implementa
     * la edición de reportes en estado BORRADOR.
     *
     * <p>Requiere {@code @Transactional} explícito porque Spring Data no lo agrega
     * automáticamente en operaciones {@code deleteBy...}.</p>
     *
     * @param reporteId ID del reporte cuyos detalles se van a eliminar
     */
    @Transactional
    void deleteByReporteId(Integer reporteId);
}
