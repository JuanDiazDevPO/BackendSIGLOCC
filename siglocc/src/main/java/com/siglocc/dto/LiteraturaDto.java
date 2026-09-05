package com.siglocc.dto;

/**
 * Balance de un tipo de literatura dentro del dashboard logístico.
 * Siempre se devuelven los 6 tipos del catálogo, ordenados por momento operativo.
 *
 * @param tipoItemId ID del tipo de ítem
 * @param codigo     código abreviado (FOLLETO, GM, MPG, EMR, LGA, NT)
 * @param recibida   unidades recibidas
 * @param entregada  unidades entregadas (actas COMPLETADA/PARCIAL)
 */
public record LiteraturaDto(
        Integer tipoItemId,
        String codigo,
        long recibida,
        long entregada
) {}
