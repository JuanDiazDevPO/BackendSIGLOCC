package com.siglocc.dto;

/**
 * Detalle de un ítem recibido en un contenedor.
 *
 * <p>Exactamente uno de {@code categoriaCajaId} o {@code tipoItemId} debe
 * ser no nulo. Si ambos son nulos o ambos tienen valor, el servicio lanza
 * {@link IllegalArgumentException}.</p>
 *
 * @param categoriaCajaId ID de la categoría de caja (NINO_2_4, NINA_5_9…); nulo si es literatura
 * @param tipoItemId      ID del tipo de ítem (FOLLETO, GM, EMR…); nulo si son cajas
 * @param cantidad        unidades recibidas (debe ser mayor que cero)
 */
public record DetalleRecepcionRequest(
        Integer categoriaCajaId,
        Integer tipoItemId,
        Integer cantidad
) {}
