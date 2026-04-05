package com.siglocc.dto;

/**
 * DTO de respuesta para el endpoint de login.
 *
 * <p>Retorna el token JWT y la información esencial del usuario autenticado
 * para que el Front-end pueda construir la sesión sin necesidad de hacer
 * consultas adicionales.</p>
 *
 * @param token   token JWT firmado que debe incluirse en el header
 *                {@code Authorization: Bearer <token>} en cada request protegido
 * @param type    tipo de token, siempre {@code "Bearer"} en este sistema
 * @param usuario información del usuario autenticado con su identidad jerárquica
 */
public record LoginResponse(
        String token,
        String type,
        UsuarioInfo usuario
) {

    /**
     * Información básica del usuario incluida en la respuesta de login.
     *
     * <p>Incluye {@code equipoId} y {@code nombreEquipo} para que el Front-end
     * pueda identificar el nivel jerárquico del usuario y construir el Dashboard
     * sin consultas adicionales.</p>
     *
     * @param id            ID del usuario en BD
     * @param nombreCompleto nombre y apellido concatenados
     * @param email         correo electrónico del usuario
     * @param rol           nombre del rol asignado (ej: "ENL_RECURSOS")
     * @param equipoId      ID del equipo al que pertenece el usuario (también en el JWT)
     * @param nombreEquipo  nombre descriptivo del equipo del usuario
     * @param detallesEquipo información completa del equipo
     */
    public record UsuarioInfo(
            Integer id,
            String nombreCompleto,
            String email,
            String rol,
            Integer equipoId,
            String nombreEquipo,
            DetallesEquipo detallesEquipo
    ) {}

    /**
     * Información del equipo al que pertenece el usuario autenticado.
     *
     * @param id     ID del equipo en BD
     * @param nombre nombre descriptivo del equipo
     * @param tipo   tipo jerárquico del equipo: {@code ENL}, {@code ERLE} o {@code ERL}
     * @param enlId  ID del equipo ENL padre; {@code null} si el equipo es ENL
     * @param erleId ID del equipo ERLE padre; {@code null} si no aplica
     */
    public record DetallesEquipo(
            Integer id,
            String nombre,
            String tipo,
            Integer enlId,
            Integer erleId
    ) {}
}
