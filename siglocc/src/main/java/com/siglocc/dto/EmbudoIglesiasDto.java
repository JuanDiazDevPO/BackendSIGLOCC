package com.siglocc.dto;

/**
 * Embudo de conversión de iglesias a lo largo de la operación logística.
 *
 * @param inscritas     total de iglesias registradas, en cualquier estado
 * @param aprobadas     iglesias en estado {@code APROBADA}
 * @param capacitadas   iglesias aprobadas con registro en {@code capacitaciones_iglesia}
 * @param conAsignacion iglesias aprobadas con línea de caja en una corrida CONFIRMADA
 * @param entregadas    iglesias aprobadas con acta {@code COMPLETADA} o {@code PARCIAL}
 */
public record EmbudoIglesiasDto(
        long inscritas,
        long aprobadas,
        long capacitadas,
        long conAsignacion,
        long entregadas
) {}
