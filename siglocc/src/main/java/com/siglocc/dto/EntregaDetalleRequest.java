package com.siglocc.dto;

/**
 * Detalle de un ítem incluido en el acta de entrega a una iglesia.
 *
 * @param tipoItemId       ID del tipo de ítem (referencia a {@code tipos_item})
 * @param cantidadEntregada número de unidades entregadas
 */
public record EntregaDetalleRequest(
        Integer tipoItemId,
        Integer cantidadEntregada
) {}
