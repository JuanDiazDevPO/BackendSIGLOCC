package com.siglocc.repository;

import com.siglocc.entity.ReporteMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla {@code reportes_mensuales}.
 *
 * <p>Incluye tres variantes de consulta de listado que replican la lógica
 * jerárquica del Dashboard: ENL ve todo, ERLE ve su clúster, ERL solo se ve a sí mismo.</p>
 */
public interface ReporteMensualRepository extends JpaRepository<ReporteMensual, Integer> {

    /**
     * Verifica si ya existe un reporte para el equipo, temporada, mes y año dados.
     * Se usa como guardia antes de insertar para retornar un error descriptivo
     * antes de que la restricción UNIQUE de la tabla lo rechace.
     *
     * @param equipoId   ID del equipo
     * @param temporadaId ID de la temporada
     * @param mes        mes del reporte (1-12)
     * @param anio       año del reporte
     * @return {@code true} si ya existe un reporte con esa combinación
     */
    boolean existsByEquipoIdAndTemporadaIdAndMesAndAnio(
            Integer equipoId, Integer temporadaId, Integer mes, Integer anio);

    /**
     * Retorna todos los reportes de una temporada. Usado por el nivel ENL para
     * obtener visibilidad completa nacional.
     *
     * @param temporadaId ID de la temporada
     * @return lista de todos los reportes de esa temporada
     */
    List<ReporteMensual> findByTemporadaId(Integer temporadaId);

    /**
     * Retorna los reportes de un equipo específico en una temporada. Usado por
     * el nivel ERL para ver únicamente sus propios reportes.
     *
     * @param equipoId    ID del equipo ERL
     * @param temporadaId ID de la temporada
     * @return lista de reportes del equipo en esa temporada
     */
    List<ReporteMensual> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);

    /**
     * Retorna los reportes del clúster de un ERLE: incluye los reportes del propio
     * equipo ERLE y los de todos los equipos ERL cuyo {@code erle_id} apunta a ese ERLE.
     *
     * <p>La condición OR no puede expresarse con métodos derivados de Spring Data,
     * por lo que se usa JPQL explícito. La subconsulta sobre {@code Equipo} usa
     * la propiedad mapeada {@code erleId} (columna {@code erle_id} en BD).</p>
     *
     * @param erleId      ID del equipo ERLE (cabeza del clúster)
     * @param temporadaId ID de la temporada
     * @return lista de reportes del ERLE y sus ERL subordinados
     */
    @Query("SELECT r FROM ReporteMensual r " +
           "WHERE (r.equipoId = :erleId " +
           "   OR r.equipoId IN (SELECT e.id FROM Equipo e WHERE e.erleId = :erleId)) " +
           "AND r.temporadaId = :temporadaId")
    List<ReporteMensual> findByErleClusterAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);
}
