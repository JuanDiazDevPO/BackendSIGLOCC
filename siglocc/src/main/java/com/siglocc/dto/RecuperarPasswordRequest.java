package com.siglocc.dto;

/**
 * DTO de entrada para solicitar el restablecimiento de contraseña.
 *
 * <p>El usuario proporciona únicamente su correo electrónico. El sistema
 * busca si el email existe en BD y, si es así, genera un token y envía
 * el enlace de recuperación.</p>
 *
 * <p><strong>Nota de seguridad:</strong> La respuesta del endpoint es siempre
 * la misma independientemente de si el email existe o no, para evitar que
 * un atacante pueda enumerar qué correos están registrados en el sistema.</p>
 *
 * @param email correo electrónico del usuario que olvidó su contraseña
 */
public record RecuperarPasswordRequest(String email) {}
