package com.siglocc.dto;

import java.time.LocalDate;

/**
 * Respuesta con los datos de una temporada operativa.
 *
 * @param id          identificador único
 * @param nombre      nombre descriptivo (ej: "Temporada 2025-2026")
 * @param fechaInicio fecha de inicio de la temporada
 * @param fechaFin    fecha de cierre de la temporada
 * @param esActual    {@code true} si es la temporada actualmente activa
 */
public record TemporadaResponse(
        Integer id,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean esActual
) {}
