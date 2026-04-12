package com.siglocc.dto;

/**
 * DTO para la acción de aprobar o rechazar un reporte mensual.
 *
 * <p>Se envía en el body de {@code PATCH /api/v1/reportes/{id}/estado}.</p>
 *
 * <p>Transiciones válidas según el tipo de equipo del revisor:</p>
 * <ul>
 *   <li><strong>ERLE:</strong> {@code PENDIENTE_ERLE → PENDIENTE_ENL} (aprobar)
 *       o {@code PENDIENTE_ERLE → RECHAZADO} (rechazar).</li>
 *   <li><strong>ENL:</strong> {@code PENDIENTE_ENL → APROBADO} (aprobar)
 *       o {@code PENDIENTE_ENL → RECHAZADO} (rechazar).</li>
 * </ul>
 *
 * <p>Ejemplo de aprobación (ERLE):</p>
 * <pre>{@code { "nuevoEstado": "PENDIENTE_ENL" }}</pre>
 *
 * <p>Ejemplo de rechazo (ENL):</p>
 * <pre>{@code { "nuevoEstado": "RECHAZADO", "observaciones": "Soportes ilegibles" }}</pre>
 *
 * @param nuevoEstado   nombre del estado destino. Debe ser un valor válido de
 *                      {@link com.siglocc.entity.EstadoReporte}.
 * @param observaciones motivo del rechazo. Obligatorio cuando {@code nuevoEstado}
 *                      es {@code "RECHAZADO"}; ignorado en otros casos.
 */
public record CambiarEstadoRequest(
        String nuevoEstado,
        String observaciones
) {}
