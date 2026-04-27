package com.siglocc.dto;

/**
 * Detalle de inventario recibido en un contenedor, enriquecido con descripciones.
 *
 * @param categoriaCajaId  ID de la categoría (nulo si es literatura)
 * @param codigoCategoria  código de la categoría (NINO_2_4, NINA_5_9…; nulo si es literatura)
 * @param descripcionCategoria descripción legible de la categoría (nulo si es literatura)
 * @param tipoItemId       ID del tipo de ítem (nulo si son cajas)
 * @param codigoItem       código del ítem (FOLLETO, GM, EMR…; nulo si son cajas)
 * @param cantidad         unidades recibidas
 */
public record DetalleRecepcionResponse(
        Integer categoriaCajaId,
        String codigoCategoria,
        String descripcionCategoria,
        Integer tipoItemId,
        String codigoItem,
        Integer cantidad
) {}
