package com.siglocc.dto;

/**
 * Avance logístico de un equipo dentro del alcance visible del usuario.
 *
 * @param equipoId       ID del equipo
 * @param nombre         nombre descriptivo del equipo
 * @param tipo           tipo jerárquico: {@code "ENL"}, {@code "ERLE"} o {@code "ERL"}
 * @param iglesias       iglesias APROBADAS del equipo
 * @param capacitadas    de esas, cuántas ya tienen capacitación registrada
 * @param cajasAsignadas cajas asignadas (CONFIRMADA) al equipo
 * @param cajasEntregadas cajas entregadas (COMPLETADA/PARCIAL) por el equipo
 */
public record AvanceEquipoDto(
        Integer equipoId,
        String nombre,
        String tipo,
        long iglesias,
        long capacitadas,
        long cajasAsignadas,
        long cajasEntregadas
) {}
