package com.siglocc.repository;

import com.siglocc.entity.ParametrosNconnect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link ParametrosNconnect}.
 *
 * <p>Permite consultar y persistir los parámetros financieros globales del ENL.
 * El método más crítico es {@link #findByTemporadaId} que se usa para validar
 * que el ENL configuró los parámetros antes de permitir la carga de presupuestos.</p>
 */
public interface ParametrosNconnectRepository extends JpaRepository<ParametrosNconnect, Integer> {

    /**
     * Busca los parámetros configurados para una temporada específica.
     *
     * @param temporadaId ID de la temporada
     * @return {@code Optional} con los parámetros si existen, vacío si el ENL
     *         no los ha configurado aún para esa temporada
     */
    Optional<ParametrosNconnect> findByTemporadaId(Integer temporadaId);
}
