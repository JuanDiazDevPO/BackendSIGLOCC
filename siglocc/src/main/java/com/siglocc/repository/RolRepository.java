package com.siglocc.repository;

import com.siglocc.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad {@link Rol}.
 *
 * <p>Utilizado en el registro de usuarios para verificar que el
 * {@code role_id} recibido en el request exista en la tabla {@code roles}.</p>
 */
public interface RolRepository extends JpaRepository<Rol, Integer> {}
