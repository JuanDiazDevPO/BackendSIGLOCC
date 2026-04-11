package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un token de un solo uso para restablecer contraseña.
 *
 * <p>Mapea la tabla {@code tokens_recuperacion}. Cada vez que un usuario solicita
 * restablecer su contraseña, se genera un registro aquí con un token UUID único
 * y una ventana de expiración de 30 minutos.</p>
 *
 * <p><strong>Ciclo de vida del token:</strong></p>
 * <ol>
 *   <li>Se crea con {@code usado = false} al solicitar la recuperación.</li>
 *   <li>Si el usuario tenía un token previo sin usar, se elimina antes de crear el nuevo
 *       (no se acumulan tokens por usuario).</li>
 *   <li>Al restablecer la contraseña exitosamente, se marca como {@code usado = true}
 *       para que no pueda reutilizarse aunque no haya expirado.</li>
 * </ol>
 *
 * <p><strong>Seguridad:</strong> El campo {@code token} tiene restricción UNIQUE en BD.
 * Se genera con {@code UUID.randomUUID()} lo que hace prácticamente imposible
 * adivinar o colisionar con otro token activo.</p>
 */
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID del usuario dueño del token.
     * Se almacena como entero simple para evitar carga lazy innecesaria.
     */
    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    /**
     * Token UUID de un solo uso enviado en el enlace del correo.
     * Ejemplo: {@code "a3f8c1d2-4e7b-4a0f-9c2e-1b3d5e7f9a0c"}
     */
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    /**
     * Fecha y hora en que el token deja de ser válido.
     * Se establece a {@code ahora + 30 minutos} al momento de la creación.
     */
    @Column(name = "expiracion", nullable = false)
    private LocalDateTime expiracion;

    /**
     * Indica si el token ya fue utilizado para restablecer la contraseña.
     * Una vez marcado como {@code true}, no puede volver a usarse aunque no haya expirado.
     */
    @Column(nullable = false)
    private Boolean usado = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiracion() { return expiracion; }
    public void setExpiracion(LocalDateTime expiracion) { this.expiracion = expiracion; }

    public Boolean getUsado() { return usado; }
    public void setUsado(Boolean usado) { this.usado = usado; }
}
