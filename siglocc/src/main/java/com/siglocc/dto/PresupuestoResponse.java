package com.siglocc.dto;

/**
 * DTO de respuesta tras registrar un presupuesto de equipo.
 *
 * @param mensaje Descripción del resultado (nombre del equipo incluido)
 * @param id      ID del registro creado en {@code presupuesto_datos}
 */
public record PresupuestoResponse(
        String mensaje,
        Integer id
) {}
