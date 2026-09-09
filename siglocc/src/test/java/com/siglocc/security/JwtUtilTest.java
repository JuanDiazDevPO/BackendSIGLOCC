package com.siglocc.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link JwtUtil}.
 *
 * <p>{@code JwtUtil} no tiene dependencias externas (no requiere Mockito): sus
 * campos {@code secret}/{@code expiration} vienen de {@code @Value} y aquí se
 * inyectan directamente con {@link ReflectionTestUtils}, igual que lo haría
 * Spring al levantar el contexto real.</p>
 */
class JwtUtilTest {

    /** Secreto de prueba de 64+ caracteres, requerido por HMAC-SHA512. */
    private static final String SECRET =
            "prueba-secreto-jwt-siglocc-1234567890-abcdefghijklmnopqrstuvwxyz-64bytes";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L); // 1 hora
    }

    // ─────────────────────────────────────────────────────────────────────
    // generateToken + extractores — round-trip de los claims
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void generateToken_extractUsername_devuelveElMismoEmail() {
        String token = jwtUtil.generateToken("erl@siglocc.org", "ENL_RECURSOS", 8, "ERL");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("erl@siglocc.org");
    }

    @Test
    void generateToken_extractEquipoId_devuelveElMismoId() {
        String token = jwtUtil.generateToken("erl@siglocc.org", "ENL_RECURSOS", 24, "ERL");

        assertThat(jwtUtil.extractEquipoId(token)).isEqualTo(24);
    }

    @Test
    void generateToken_extractEquipoTipo_devuelveElMismoTipo() {
        String token = jwtUtil.generateToken("erle@siglocc.org", "ENL_LOGISTICA", 2, "ERLE");

        assertThat(jwtUtil.extractEquipoTipo(token)).isEqualTo("ERLE");
    }

    // ─────────────────────────────────────────────────────────────────────
    // isTokenValid
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void isTokenValid_conUsuarioCorrectoYNoExpirado_retornaTrue() {
        String token = jwtUtil.generateToken("enl@siglocc.org", "ENL_RECURSOS", 1, "ENL");

        assertThat(jwtUtil.isTokenValid(token, "enl@siglocc.org")).isTrue();
    }

    @Test
    void isTokenValid_conUsuarioDistintoAlDelToken_retornaFalse() {
        String token = jwtUtil.generateToken("enl@siglocc.org", "ENL_RECURSOS", 1, "ENL");

        assertThat(jwtUtil.isTokenValid(token, "otro@siglocc.org")).isFalse();
    }

    /**
     * JJWT ya valida la expiración al parsear el token (dentro de
     * {@code extractUsername}), antes de que {@code isTokenValid} llegue a
     * evaluar su propio chequeo de expiración. Por eso un token vencido no
     * retorna {@code false}: la excepción se lanza primero. En producción esto
     * es inofensivo porque {@link JwtAuthFilter} envuelve toda la validación en
     * un {@code catch (Exception e)} y trata cualquier fallo como "no autenticado",
     * pero es importante documentar el comportamiento real del método.
     */
    @Test
    void isTokenValid_conTokenExpirado_lanzaExpiredJwtException() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1_000L); // ya vencido al generarse
        String token = jwtUtil.generateToken("enl@siglocc.org", "ENL_RECURSOS", 1, "ENL");

        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, "enl@siglocc.org"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Integridad del token — firma y formato
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void extractUsername_conTokenFirmadoConOtroSecreto_lanzaSignatureException() {
        String token = jwtUtil.generateToken("enl@siglocc.org", "ENL_RECURSOS", 1, "ENL");

        JwtUtil otroJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otroJwtUtil, "secret",
                "otro-secreto-completamente-distinto-1234567890-abcdefghijklmnop-64b");
        ReflectionTestUtils.setField(otroJwtUtil, "expiration", 3_600_000L);

        assertThatThrownBy(() -> otroJwtUtil.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void extractUsername_conTokenMalformado_lanzaMalformedJwtException() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("esto-no-es-un-jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }
}
