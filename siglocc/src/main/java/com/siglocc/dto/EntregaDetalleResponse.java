package com.siglocc.dto;

/**
 * Detalle de un ítem en el acta de entrega, enriquecido con código y nombre.
 *
 * @param tipoItemId       ID del tipo de ítem
 * @param codigoItem       código abreviado (OE, NT, EMR, LGA)
 * @param nombreItem       nombre completo del ítem
 * @param cantidadEntregada cantidad de unidades entregadas
 */
public record EntregaDetalleResponse(
        Integer tipoItemId,
        String codigoItem,
        String nombreItem,
        Integer cantidadEntregada
) {}
