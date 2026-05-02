package com.siglocc.dto;

import java.time.LocalDate;

/**
 * Respuesta con los datos de una capacitación de iglesia.
 *
 * @param id                identificador único
 * @param iglesiaId         ID de la iglesia
 * @param temporadaId       ID de la temporada
 * @param fechaCapacitacion fecha de la capacitación
 * @param maestrosEnviados  número de maestros que asistieron
 * @param cajasCalculadas   cajas asignadas (maestrosEnviados × 25)
 * @param gmEntregados      Guías Ministeriales entregadas
 * @param mpgEntregados     libros MPG entregados
 * @param observaciones     observaciones de la jornada
 */
public record CapacitacionResponse(
        Integer id,
        Integer iglesiaId,
        Integer temporadaId,
        LocalDate fechaCapacitacion,
        Integer maestrosEnviados,
        Integer cajasCalculadas,
        Integer gmEntregados,
        Integer mpgEntregados,
        String observaciones
) {}
