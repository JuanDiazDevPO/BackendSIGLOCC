package com.siglocc.security;

/**
 * Señala que el token JWT autenticado no trae la identidad jerárquica esperada
 * ({@code equipoId}/{@code equipoTipo} en {@link JwtAuthDetails}).
 *
 * <p>En la práctica solo ocurre si el filtro de seguridad dejó pasar una request
 * autenticada sin inyectar {@link JwtAuthDetails} en el
 * {@code SecurityContextHolder} — la señal correcta para el cliente es "vuelve
 * a iniciar sesión", no un conflicto de estado de negocio. Por eso se modela
 * como un tipo dedicado en vez de un {@link IllegalStateException} genérico:
 * así el manejador global puede responder {@code 400} solo para este caso,
 * sin reclasificar los demás {@code IllegalStateException} de conflicto de
 * estado/permisos (que siguen respondiendo {@code 409}).</p>
 */
public class IdentidadJwtException extends IllegalStateException {

    public IdentidadJwtException(String mensaje) {
        super(mensaje);
    }
}
