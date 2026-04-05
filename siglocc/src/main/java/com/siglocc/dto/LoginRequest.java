package com.siglocc.dto;

/**
 * DTO de entrada para el endpoint de login.
 *
 * <p>Contiene las credenciales que el usuario envía desde el Front-end.
 * El login en SIGLOCC se realiza con correo electrónico y contraseña.</p>
 *
 * @param email    correo electrónico registrado del usuario
 * @param password contraseña en texto plano (se compara con el hash BCrypt en BD)
 */
public record LoginRequest(String email, String password) {}
