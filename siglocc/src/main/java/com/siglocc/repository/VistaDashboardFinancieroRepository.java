package com.siglocc.repository;

import com.siglocc.entity.VistaDashboardFinanciero;
import com.siglocc.entity.VistaDashboardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA de solo lectura para la vista {@code vista_dashboard_financiero}.
 *
 * <p>Expone tres queries, uno por cada nivel jerárquico del sistema de mando.
 * El servicio elige cuál ejecutar según el {@code equipoTipo} extraído del JWT,
 * garantizando que cada usuario solo acceda a los datos que le corresponden.</p>
 *
 * <p><strong>Algoritmo de filtrado por nivel:</strong></p>
 * <ul>
 *   <li><strong>ENL</strong>: Ve todos los equipos de la temporada (visión 360°).</li>
 *   <li><strong>ERLE</strong>: Ve su propia fila más todas las filas de ERL
 *       donde {@code erle_id = equipoId}. No puede ver datos de otras regiones.</li>
 *   <li><strong>ERL</strong>: Ve únicamente su propia fila.</li>
 * </ul>
 */
public interface VistaDashboardFinancieroRepository
        extends JpaRepository<VistaDashboardFinanciero, VistaDashboardId> {

    /**
     * Consulta para el nivel ENL: retorna todos los equipos de una temporada.
     *
     * <p>Permite al coordinador nacional tener una visión 360° del país:
     * ve la sumatoria consolidada y el detalle de cada ERLE y ERL.</p>
     *
     * @param temporadaId ID de la temporada a consultar
     * @return lista completa de todos los equipos en la temporada
     */
    @Query("SELECT v FROM VistaDashboardFinanciero v WHERE v.id.temporadaId = :temporadaId")
    List<VistaDashboardFinanciero> findAllByTemporada(@Param("temporadaId") Integer temporadaId);

    /**
     * Consulta para el nivel ERLE: retorna su propio registro más los de sus ERL.
     *
     * <p>Un ERLE supervisa un clúster regional. Puede ver su propia ejecución
     * ({@code equipo_id = erleId}) y la de todos los ERL bajo su cargo
     * ({@code erle_id = erleId}). No puede ver otros departamentos.</p>
     *
     * @param erleId      ID del equipo ERLE que hace la consulta
     * @param temporadaId ID de la temporada a consultar
     * @return registros del ERLE y sus ERL subordinados
     */
    @Query("""
            SELECT v FROM VistaDashboardFinanciero v
            WHERE v.id.temporadaId = :temporadaId
              AND (v.id.equipoId = :erleId OR v.erleId = :erleId)
            """)
    List<VistaDashboardFinanciero> findByErleAndTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);

    /**
     * Consulta para el nivel ERL: retorna únicamente el registro propio del equipo.
     *
     * <p>El ERL solo puede ver sus propios números para saber cuánto le queda
     * disponible para operar. No tiene acceso a datos de otros equipos.</p>
     *
     * @param equipoId    ID del equipo ERL que hace la consulta
     * @param temporadaId ID de la temporada a consultar
     * @return lista con el único registro del equipo (o vacía si aún no tiene presupuesto)
     */
    @Query("""
            SELECT v FROM VistaDashboardFinanciero v
            WHERE v.id.equipoId = :equipoId
              AND v.id.temporadaId = :temporadaId
            """)
    List<VistaDashboardFinanciero> findByEquipoAndTemporada(
            @Param("equipoId") Integer equipoId,
            @Param("temporadaId") Integer temporadaId);
}
