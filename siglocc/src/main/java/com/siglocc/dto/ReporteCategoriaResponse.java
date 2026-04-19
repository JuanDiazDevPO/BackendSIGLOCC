package com.siglocc.dto;

/**
 * Respuesta con los datos de una categoría de gasto para reportes mensuales.
 *
 * @param codigo     código único de la categoría (ej: "E-1", "M-2", "O-1")
 * @param familia    familia a la que pertenece: E (Entrenamiento), M (Mentoría) u O (Otros)
 * @param nombreLargo descripción completa y legible de la categoría
 */
public record ReporteCategoriaResponse(
        String codigo,
        String familia,
        String nombreLargo
) {}
