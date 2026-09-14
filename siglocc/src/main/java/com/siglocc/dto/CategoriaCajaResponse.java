package com.siglocc.dto;

/**
 * Categoría de caja OCC (género × rango de edad) para poblar selects del front.
 *
 * @param id          ID a enviar como {@code categoriaCajaId} en recepciones/asignaciones/entregas
 * @param codigo      código abreviado (ej: {@code NINO_2_4})
 * @param genero      {@code NINO} o {@code NINA}
 * @param edadMin     edad mínima del rango (inclusive)
 * @param edadMax     edad máxima del rango (inclusive)
 * @param descripcion descripción legible (ej: «Niño 5-9 años»)
 */
public record CategoriaCajaResponse(
        Integer id,
        String codigo,
        String genero,
        Integer edadMin,
        Integer edadMax,
        String descripcion
) {}
