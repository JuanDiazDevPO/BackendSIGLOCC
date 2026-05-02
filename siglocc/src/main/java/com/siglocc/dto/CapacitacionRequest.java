package com.siglocc.dto;

import java.time.LocalDate;

/**
 * Datos para registrar la capacitación de los maestros de una iglesia (Momento 2).
 *
 * <p>El número de cajas se calcula automáticamente como
 * {@code maestrosEnviados × 25}. No es necesario enviarlo en el request.</p>
 *
 * @param iglesiaId         ID de la iglesia cuya delegación asistió
 * @param temporadaId       ID de la temporada
 * @param fechaCapacitacion fecha de la jornada de capacitación
 * @param maestrosEnviados  número de maestros que asistieron
 * @param gmEntregados      Guías Ministeriales entregadas (normalmente = maestrosEnviados)
 * @param mpgEntregados     libros MPG entregados (normalmente = maestrosEnviados)
 * @param observaciones     observaciones opcionales de la jornada
 */
public record CapacitacionRequest(
        Integer iglesiaId,
        Integer temporadaId,
        LocalDate fechaCapacitacion,
        Integer maestrosEnviados,
        Integer gmEntregados,
        Integer mpgEntregados,
        String observaciones
) {}
