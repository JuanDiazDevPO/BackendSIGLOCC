package com.siglocc.dto;

/**
 * DTO de entrada para confirmar el restablecimiento de contraseña.
 *
 * <p>El Front-end envía este request cuando el usuario hace clic en el enlace
 * del correo, ingresa su nueva contraseña y confirma. El campo {@code token}
 * proviene del parámetro {@code ?token=...} en la URL del enlace.</p>
 *
 * <p><strong>Validaciones que aplica el servicio:</strong></p>
 * <ul>
 *   <li>El token debe existir en BD.</li>
 *   <li>El token no debe haber expirado (ventana de 30 minutos).</li>
 *   <li>El token no debe haber sido usado previamente.</li>
 * </ul>
 *
 * @param token          UUID extraído del enlace del correo de recuperación
 * @param nuevaPassword  nueva contraseña elegida por el usuario (se almacenará con BCrypt)
 */
public record RestablecerPasswordRequest(String token, String nuevaPassword) {}
