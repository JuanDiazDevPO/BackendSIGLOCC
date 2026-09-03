package com.siglocc.repository;

import com.siglocc.entity.SolicitudAnticipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link SolicitudAnticipo}.
 *
 * <p>Gestiona la persistencia de las solicitudes de anticipo.</p>
 */
public interface SolicitudAnticipoRepository extends JpaRepository<SolicitudAnticipo, Integer> {

    /**
     * Retorna todas las solicitudes ordenadas de la más reciente a la más antigua.
     * Usado por la bandeja de aprobación de {@code ENL_RECURSOS}.
     *
     * @return lista completa de solicitudes, orden descendente por fecha
     */
    List<SolicitudAnticipo> findAllByOrderByFechaSolicitudDesc();
}
