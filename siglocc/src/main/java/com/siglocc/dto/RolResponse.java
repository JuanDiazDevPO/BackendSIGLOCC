package com.siglocc.dto;

/**
 * DTO de respuesta para un rol del sistema.
 *
 * @param id     identificador del rol
 * @param nombre nombre del rol (ej: {@code "ENL_RECURSOS"}, {@code "ERLE"})
 */
public record RolResponse(Integer id, String nombre) {}
