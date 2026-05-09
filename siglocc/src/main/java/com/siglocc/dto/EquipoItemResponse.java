package com.siglocc.dto;

/**
 * DTO de respuesta para un equipo, usado en listados de selección.
 *
 * @param id     identificador del equipo
 * @param nombre nombre descriptivo del equipo (ej: {@code "Equipo Regional de Santander"})
 * @param tipo   nivel jerárquico: {@code "ENL"}, {@code "ERLE"} o {@code "ERL"}
 */
public record EquipoItemResponse(Integer id, String nombre, String tipo) {}
