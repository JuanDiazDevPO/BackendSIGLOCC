package com.siglocc.dto;

/**
 * Solicitud para ajustar manualmente una línea de asignación.
 *
 * <p>El coordinador puede modificar la cantidad asignada a una iglesia
 * para una categoría o ítem específico después de la generación automática,
 * mientras la cabecera esté en estado {@code BORRADOR}.</p>
 *
 * @param iglesiaId       ID de la iglesia cuya línea se ajusta
 * @param categoriaCajaId ID de la categoría de caja a ajustar (nulo si es literatura)
 * @param tipoItemId      ID del tipo de ítem a ajustar (nulo si son cajas)
 * @param nuevaCantidad   nueva cantidad asignada (0 o más)
 */
public record AjusteAsignacionRequest(
        Integer iglesiaId,
        Integer categoriaCajaId,
        Integer tipoItemId,
        Integer nuevaCantidad
) {}
