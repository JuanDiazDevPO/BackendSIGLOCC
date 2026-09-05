package com.siglocc.dto;

/**
 * Detalle de un ítem incluido en el acta de entrega a una iglesia.
 *
 * <p>Exactamente uno de {@code categoriaCajaId} o {@code tipoItemId} debe
 * ser no nulo — mismo patrón que {@code DetalleRecepcionRequest} y
 * {@code AjusteAsignacionRequest}. Si ambos son nulos o ambos tienen valor,
 * el servicio lanza {@link IllegalArgumentException}.</p>
 *
 * @param categoriaCajaId  ID de la categoría de caja entregada (NINO_2_4, NINA_5_9…); nulo si es literatura
 * @param tipoItemId       ID del tipo de ítem entregado (FOLLETO, GM, EMR…); nulo si son cajas
 * @param cantidadEntregada número de unidades entregadas
 */
public record EntregaDetalleRequest(
        Integer categoriaCajaId,
        Integer tipoItemId,
        Integer cantidadEntregada
) {}
