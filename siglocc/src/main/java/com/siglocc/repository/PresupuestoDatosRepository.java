package com.siglocc.repository;

import com.siglocc.entity.PresupuestoDatos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link PresupuestoDatos}.
 *
 * <p>Gestiona la persistencia de los datos operativos de presupuesto ingresados
 * por cada equipo. La combinación equipo + temporada identifica unívocamente
 * un registro de presupuesto.</p>
 */
public interface PresupuestoDatosRepository extends JpaRepository<PresupuestoDatos, Integer> {

    /**
     * Busca los datos de presupuesto de un equipo en una temporada específica.
     * Útil para verificar si ya existe un registro antes de crear uno nuevo.
     *
     * @param equipoId    ID del equipo
     * @param temporadaId ID de la temporada
     * @return {@code Optional} con los datos si ya fueron ingresados
     */
    Optional<PresupuestoDatos> findByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);
}
