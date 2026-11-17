package com.siglocc.repository;

import com.siglocc.entity.SolicitudAnticipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link SolicitudAnticipo}.
 *
 * <p>Gestiona la persistencia de las solicitudes de anticipo. Las dos
 * consultas de listado replican el filtrado jerárquico de
 * {@code ReporteMensualRepository}: ENL ve todo, ERLE ve su propio clúster.
 * El parámetro {@code temporadaId} es opcional (puede ser {@code null}).</p>
 */
public interface SolicitudAnticipoRepository extends JpaRepository<SolicitudAnticipo, Integer> {

    /**
     * Retorna todas las solicitudes, opcionalmente filtradas por temporada.
     * Usado por la bandeja nacional de {@code ENL_RECURSOS}.
     *
     * @param temporadaId ID de la temporada, o {@code null} para no filtrar
     * @return lista de solicitudes, orden descendente por fecha
     */
    @Query("SELECT s FROM SolicitudAnticipo s " +
           "WHERE (:temporadaId IS NULL OR s.temporadaId = :temporadaId) " +
           "ORDER BY s.fechaSolicitud DESC")
    List<SolicitudAnticipo> findAllOpcionalmentePorTemporada(@Param("temporadaId") Integer temporadaId);

    /**
     * Retorna las solicitudes del clúster de un ERLE (el propio equipo ERLE
     * más todos los ERL cuyo {@code erle_id} apunta a él), opcionalmente
     * filtradas por temporada.
     *
     * @param erleId      ID del equipo ERLE (cabeza del clúster)
     * @param temporadaId ID de la temporada, o {@code null} para no filtrar
     * @return lista de solicitudes del ERLE y sus ERL subordinados
     */
    @Query("SELECT s FROM SolicitudAnticipo s " +
           "WHERE (s.equipoId = :erleId " +
           "   OR s.equipoId IN (SELECT e.id FROM Equipo e WHERE e.erleId = :erleId)) " +
           "AND (:temporadaId IS NULL OR s.temporadaId = :temporadaId) " +
           "ORDER BY s.fechaSolicitud DESC")
    List<SolicitudAnticipo> findByErleClusterOpcionalmentePorTemporada(
            @Param("erleId") Integer erleId,
            @Param("temporadaId") Integer temporadaId);
}
