package com.siglocc.dto;

/**
 * Embudo del flujo físico de cajas OCC a lo largo de la operación logística.
 *
 * @param solicitadas suma de cajas solicitadas por las iglesias {@code APROBADA}
 * @param recibidas   suma de cajas recibidas en contenedores
 * @param asignadas   suma de cajas asignadas en corridas {@code CONFIRMADA}
 * @param entregadas  suma de cajas entregadas en actas {@code COMPLETADA} o {@code PARCIAL}
 */
public record EmbudoCajasDto(
        long solicitadas,
        long recibidas,
        long asignadas,
        long entregadas
) {}
