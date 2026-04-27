package com.siglocc.dto;

/**
 * Línea de asignación: iglesia × categoría/ítem × cantidad asignada.
 *
 * @param iglesiaId            ID de la iglesia receptora
 * @param nombreIglesia        nombre de la iglesia
 * @param categoriaCajaId      ID de la categoría de caja (nulo si es literatura)
 * @param codigoCategoria      código de la categoría (NINO_2_4…; nulo si es literatura)
 * @param descripcionCategoria descripción de la categoría (nulo si es literatura)
 * @param tipoItemId           ID del tipo de literatura (nulo si son cajas)
 * @param codigoItem           código del ítem (EMR, LGA, NT…; nulo si son cajas)
 * @param cantidadAsignada     unidades asignadas a esta iglesia
 * @param ajustadaManualmente  true si el coordinador modificó la cantidad automática
 */
public record AsignacionDetalleResponse(
        Integer iglesiaId,
        String nombreIglesia,
        Integer categoriaCajaId,
        String codigoCategoria,
        String descripcionCategoria,
        Integer tipoItemId,
        String codigoItem,
        Integer cantidadAsignada,
        Boolean ajustadaManualmente
) {}
