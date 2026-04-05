package com.siglocc.repository;

import com.siglocc.entity.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Temporada}.
 *
 * <p>El método principal de este repositorio es {@code findByEsActualTrue},
 * que permite obtener la temporada vigente sin necesidad de conocer su ID.
 * El módulo de anticipos lo usa automáticamente al crear solicitudes.</p>
 */
public interface TemporadaRepository extends JpaRepository<Temporada, Integer> {

    /**
     * Busca la temporada marcada como activa ({@code es_actual = true}).
     * Se espera que solo exista una temporada activa a la vez.
     *
     * @return un {@link Optional} con la temporada activa, vacío si no hay ninguna configurada
     */
    Optional<Temporada> findByEsActualTrue();
}
