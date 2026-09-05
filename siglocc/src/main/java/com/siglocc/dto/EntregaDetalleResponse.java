package com.siglocc.dto;

/**
 * Detalle de un ítem en el acta de entrega, enriquecido con código y nombre.
 *
 * <p>Mismo patrón dual que {@code DetalleRecepcionResponse} y
 * {@code AsignacionDetalleResponse}: exactamente uno de los dos bloques
 * (categoría de caja / tipo de ítem) viene poblado por fila.</p>
 *
 * @param categoriaCajaId      ID de la categoría de caja (nulo si es literatura)
 * @param codigoCategoria      código de la categoría (NINO_2_4, NINA_5_9…; nulo si es literatura)
 * @param descripcionCategoria descripción legible de la categoría (nulo si es literatura)
 * @param tipoItemId           ID del tipo de ítem (nulo si son cajas)
 * @param codigoItem           código del ítem (FOLLETO, GM, EMR…; nulo si son cajas)
 * @param nombreItem           nombre completo del ítem (nulo si son cajas)
 * @param cantidadEntregada    cantidad de unidades entregadas
 */
public record EntregaDetalleResponse(
        Integer categoriaCajaId,
        String codigoCategoria,
        String descripcionCategoria,
        Integer tipoItemId,
        String codigoItem,
        String nombreItem,
        Integer cantidadEntregada
) {}
