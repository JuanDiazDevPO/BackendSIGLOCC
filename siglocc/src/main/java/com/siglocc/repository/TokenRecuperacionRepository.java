package com.siglocc.repository;

import com.siglocc.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link TokenRecuperacion}.
 *
 * <p>Gestiona la persistencia y consulta de tokens de recuperación de contraseña.
 * Los dos métodos clave son:</p>
 * <ul>
 *   <li>{@link #findByToken} – usado al validar el enlace del correo.</li>
 *   <li>{@link #deleteByUsuarioId} – limpia tokens anteriores antes de emitir uno nuevo,
 *       garantizando que cada usuario tenga como máximo un token activo a la vez.</li>
 * </ul>
 */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Integer> {

    /**
     * Busca un token de recuperación por su valor UUID.
     *
     * <p>Usado al procesar el enlace del correo: el usuario llega con el token
     * en la URL y el backend lo busca aquí para validar si es vigente y no usado.</p>
     *
     * @param token valor UUID del token recibido en la URL
     * @return {@code Optional} con el token si existe en BD, vacío si no
     */
    Optional<TokenRecuperacion> findByToken(String token);

    /**
     * Elimina todos los tokens de recuperación asociados a un usuario.
     *
     * <p>Se ejecuta antes de generar un nuevo token para evitar que un usuario
     * acumule múltiples enlaces activos en su correo. Solo el último enlace enviado
     * será válido.</p>
     *
     * <p>Requiere {@code @Transactional} porque Spring Data genera una operación
     * DML directa ({@code DELETE}) en lugar de cargar la entidad primero.</p>
     *
     * @param usuarioId ID del usuario cuyos tokens se van a limpiar
     */
    @Transactional
    void deleteByUsuarioId(Integer usuarioId);
}
