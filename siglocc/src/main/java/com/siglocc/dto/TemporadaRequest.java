package com.siglocc.dto;

import java.time.LocalDate;

/**
 * DTO de entrada para crear o editar una temporada.
 *
 * <p>No incluye {@code esActual} — activar una temporada es una acción aparte
 * ({@code PATCH /api/v1/temporadas/{id}/activar}) para que nunca quede más de
 * una marcada como actual por accidente.</p>
 *
 * @param nombre      nombre descriptivo (ej: "Temporada 2025-2026")
 * @param fechaInicio fecha de inicio de la temporada
 * @param fechaFin    fecha de cierre de la temporada; debe ser posterior a {@code fechaInicio}
 */
public record TemporadaRequest(
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {}
