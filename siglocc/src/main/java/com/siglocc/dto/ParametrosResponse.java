package com.siglocc.dto;

/**
 * DTO de respuesta tras guardar o actualizar los parámetros del ENL.
 *
 * @param id          ID del registro guardado en {@code parametros_nconnect}
 * @param temporadaId ID de la temporada para la que se configuraron
 * @param mensaje     Descripción del resultado (creado o actualizado)
 */
public record ParametrosResponse(
        Integer id,
        Integer temporadaId,
        String mensaje
) {}
