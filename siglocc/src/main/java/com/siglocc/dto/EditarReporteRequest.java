package com.siglocc.dto;

import java.util.List;

/**
 * DTO para la corrección de un reporte mensual rechazado.
 *
 * <p>Se envía en el body de {@code PUT /api/v1/reportes/{id}} cuando el ERL
 * necesita corregir un reporte que fue rechazado por el ERLE o el ENL.</p>
 *
 * <p>Solo se pueden modificar los rubros y sus montos. El período
 * ({@code mes}, {@code anio}, {@code temporadaId}) y el equipo no cambian,
 * ya que identifican de forma única el reporte en la base de datos.</p>
 *
 * <p>Al guardar la corrección, el sistema:</p>
 * <ol>
 *   <li>Elimina los detalles anteriores y guarda los nuevos.</li>
 *   <li>Borra el soporte adjunto (el archivo ya no refleja los montos corregidos).</li>
 *   <li>Resetea el estado a {@code BORRADOR} para que el ERL adjunte un nuevo soporte.</li>
 *   <li>Limpia los campos de auditoría ({@code aprobadorErleId}, {@code aprobadorEnlId}).</li>
 *   <li>Conserva las {@code observaciones} del rechazo para que el ERL sepa qué corregir.</li>
 * </ol>
 *
 * @param detalles nueva lista de rubros corregidos con sus montos.
 */
public record EditarReporteRequest(
        List<ReporteDetalleRequest> detalles
) {
    /**
     * Constructor compacto que hace una copia defensiva de la lista de detalles.
     */
    public EditarReporteRequest {
        detalles = List.copyOf(detalles);
    }
}
