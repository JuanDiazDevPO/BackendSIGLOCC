package com.siglocc.security;

/**
 * Contenedor de la identidad jerárquica del usuario extraída del token JWT.
 *
 * <p>Se adjunta como {@code details} al objeto {@link
 * org.springframework.security.authentication.UsernamePasswordAuthenticationToken}
 * en el {@link JwtAuthFilter}, una vez que el token es validado.</p>
 *
 * <p>Al almacenar estos datos en el contexto de seguridad de Spring, los servicios
 * que necesiten saber <em>qué equipo</em> y <em>qué nivel jerárquico</em> tiene el
 * usuario no necesitan ir a la base de datos: extraen la información directamente
 * del {@code SecurityContextHolder}, siguiendo el principio de "no confiar en
 * parámetros enviados por el cliente".</p>
 *
 * <p><strong>Uso en servicios:</strong></p>
 * <pre>{@code
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
 * Integer equipoId = details.equipoId();
 * String equipoTipo = details.equipoTipo(); // "ENL", "ERLE" o "ERL"
 * }</pre>
 *
 * @param equipoId   ID del equipo al que pertenece el usuario autenticado
 * @param equipoTipo tipo jerárquico del equipo: {@code "ENL"}, {@code "ERLE"} o {@code "ERL"}
 */
public record JwtAuthDetails(Integer equipoId, String equipoTipo) {}
