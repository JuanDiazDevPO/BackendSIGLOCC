package com.siglocc.repository;

import com.siglocc.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Spring Data genera automáticamente la implementación en tiempo de arranque.
 * Solo es necesario declarar los métodos de consulta personalizados; los
 * métodos CRUD básicos ({@code save}, {@code findById}, {@code findAll},
 * {@code deleteById}, etc.) los hereda de {@link JpaRepository}.</p>
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su correo electrónico.
     * Usado en el login y en la carga del usuario autenticado desde el token JWT.
     *
     * @param email correo electrónico del usuario
     * @return un {@link Optional} con el usuario si existe, vacío si no
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca el primer usuario que tenga el rol con el nombre indicado.
     * Se usa para encontrar al aprobador ({@code ENL_RECURSOS}) al que se le
     * notifica cuando llega una nueva solicitud de anticipo.
     *
     * @param rolName nombre del rol (ej: "ENL_RECURSOS")
     * @return un {@link Optional} con el usuario encontrado, vacío si ninguno tiene ese rol
     */
    Optional<Usuario> findFirstByRol_Name(String rolName);
}
