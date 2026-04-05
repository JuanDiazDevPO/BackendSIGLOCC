package com.siglocc.repository;

import com.siglocc.entity.MetaEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad {@link MetaEquipo}.
 *
 * <p>Permite consultar las metas de contenedores asignadas a cada equipo por temporada.
 * Se usa en el servicio de presupuesto para validar que el equipo tiene una meta
 * asignada antes de permitir la ingesta de sus datos operativos.</p>
 */
public interface MetaEquipoRepository extends JpaRepository<MetaEquipo, Integer> {

    /**
     * Verifica si un equipo tiene meta asignada para una temporada dada.
     *
     * @param equipoId    ID del equipo
     * @param temporadaId ID de la temporada
     * @return {@code true} si existe una meta registrada para esa combinación
     */
    boolean existsByEquipoIdAndTemporadaId(Integer equipoId, Integer temporadaId);
}
