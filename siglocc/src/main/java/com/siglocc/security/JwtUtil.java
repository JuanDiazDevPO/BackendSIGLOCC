package com.siglocc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para la generación y validación de tokens JWT.
 *
 * <p>Centraliza toda la lógica relacionada con JWT para que el resto de la
 * aplicación no necesite conocer los detalles del algoritmo ni la clave secreta.</p>
 *
 * <p>Configuración (en {@code application.properties}):</p>
 * <ul>
 *   <li>{@code jwt.secret} – clave secreta usada para firmar los tokens (mínimo 32 caracteres)</li>
 *   <li>{@code jwt.expiration} – tiempo de expiración en milisegundos (ej: 86400000 = 24 horas)</li>
 * </ul>
 *
 * <p>El algoritmo de firma usado es HMAC-SHA512 (elegido automáticamente por
 * la librería JJWT según el tamaño de la clave).</p>
 *
 * <p><strong>Claims incluidos en el token:</strong></p>
 * <ul>
 *   <li>{@code sub} – email del usuario (identidad principal)</li>
 *   <li>{@code rol} – nombre del rol (ej: "ENL_RECURSOS")</li>
 *   <li>{@code equipoId} – ID del equipo al que pertenece el usuario</li>
 *   <li>{@code equipoTipo} – tipo jerárquico del equipo: "ENL", "ERLE" o "ERL"</li>
 * </ul>
 *
 * <p>Los claims {@code equipoId} y {@code equipoTipo} son la identidad jerárquica
 * del Dashboard: el Backend los extrae en cada petición para determinar qué datos
 * mostrar, sin confiar en parámetros enviados por el cliente.</p>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Construye la clave criptográfica a partir del secreto configurado.
     * Se llama internamente en cada operación de firma o verificación.
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT firmado con la identidad completa del usuario,
     * incluyendo su posición en la jerarquía de equipos.
     *
     * @param username   email del usuario autenticado (subject)
     * @param rol        nombre del rol asignado (ej: "ENL_RECURSOS")
     * @param equipoId   ID del equipo al que pertenece el usuario
     * @param equipoTipo tipo jerárquico del equipo ("ENL", "ERLE" o "ERL")
     * @return token JWT en formato compacto (Base64url.Base64url.Base64url)
     */
    public String generateToken(String username, String rol, Integer equipoId, String equipoTipo) {
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .claim("equipoId", equipoId)
                .claim("equipoTipo", equipoTipo)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * Extrae el email del usuario (subject) contenido en el token.
     *
     * @param token token JWT válido
     * @return email del usuario
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae el ID del equipo del usuario desde el claim {@code equipoId} del token.
     *
     * @param token token JWT válido
     * @return ID del equipo del usuario autenticado
     */
    public Integer extractEquipoId(String token) {
        return getClaims(token).get("equipoId", Integer.class);
    }

    /**
     * Extrae el tipo jerárquico del equipo desde el claim {@code equipoTipo} del token.
     *
     * @param token token JWT válido
     * @return tipo del equipo: "ENL", "ERLE" o "ERL"
     */
    public String extractEquipoTipo(String token) {
        return getClaims(token).get("equipoTipo", String.class);
    }

    /**
     * Valida que el token pertenece al usuario indicado y no ha expirado.
     *
     * @param token    token JWT a validar
     * @param username email del usuario contra el que se verifica
     * @return {@code true} si el token es válido y vigente; {@code false} en caso contrario
     */
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    /**
     * Verifica si el token ya superó su fecha de expiración.
     */
    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parsea el token y retorna el payload (claims).
     * Lanza excepción de JJWT si el token está mal formado o la firma no coincide.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
