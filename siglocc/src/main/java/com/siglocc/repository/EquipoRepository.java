package com.siglocc.repository;

import com.siglocc.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad {@link Equipo}.
 *
 * <p>Utilizado en el registro de usuarios para verificar que el
 * {@code equipo_id} recibido en el request exista en la tabla {@code equipos}.</p>
 */
public interface EquipoRepository extends JpaRepository<Equipo, Integer> {}
