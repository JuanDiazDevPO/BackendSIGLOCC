package com.siglocc.dto;

/**
 * Balance de una categoría de caja OCC dentro del dashboard logístico.
 * Siempre se devuelven las 6 categorías del catálogo, con ceros si no hay movimiento.
 *
 * @param categoriaCajaId ID de la categoría
 * @param codigo          código abreviado (NINO_2_4, NINA_5_9…)
 * @param descripcion     descripción legible (ej: "Niño 2-4 años")
 * @param recibidas       cajas recibidas de esta categoría
 * @param asignadas       cajas asignadas (CONFIRMADA) de esta categoría
 * @param entregadas      cajas entregadas (COMPLETADA/PARCIAL) de esta categoría
 */
public record InventarioCategoriaDto(
        Integer categoriaCajaId,
        String codigo,
        String descripcion,
        long recibidas,
        long asignadas,
        long entregadas
) {}
