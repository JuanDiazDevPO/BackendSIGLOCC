package com.siglocc.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta completo de un reporte mensual.
 *
 * <p>Se retorna en todos los endpoints del módulo de reportes:
 * creación, subida de soporte, listado y cambio de estado.</p>
 *
 * <p>El campo {@code montoTotal} es la suma de todos los {@code montoGastado}
 * de la lista {@code detalles}. Se calcula en el servicio para evitar
 * que el front-end tenga que agregarlo.</p>
 *
 * @param id                   ID autoincremental del reporte.
 * @param equipoId             ID del equipo que generó el reporte.
 * @param temporadaId          ID de la temporada.
 * @param mes                  mes del período (1-12).
 * @param anio                 año del período.
 * @param urlSoporte           nombre del archivo de evidencia almacenado,
 *                             o {@code null} si aún no se ha subido.
 * @param estado               estado actual del reporte (p. ej. {@code "PENDIENTE_ERLE"}).
 * @param observaciones        motivo de rechazo o comentario del revisor, o {@code null}.
 * @param fechaCreacion        momento en que se creó el reporte.
 * @param fechaAprobacionFinal momento en que el ENL lo aprobó, o {@code null} si no aplica.
 * @param aprobadorErleId      ID del usuario ERLE que dio el primer visto bueno,
 *                             o {@code null} si aún no ha pasado ese paso.
 * @param aprobadorEnlId       ID del usuario ENL que cerró el proceso,
 *                             o {@code null} si aún no ha pasado ese paso.
 * @param detalles             lista de rubros con nombre, familia y monto.
 * @param montoTotal           suma de todos los montos reportados, en COP.
 */
public record ReporteResponse(
        Integer id,
        Integer equipoId,
        Integer temporadaId,
        Integer mes,
        Integer anio,
        String urlSoporte,
        String estado,
        String observaciones,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaAprobacionFinal,
        Integer aprobadorErleId,
        Integer aprobadorEnlId,
        List<ReporteDetalleResponse> detalles,
        BigDecimal montoTotal
) {
    /**
     * Constructor compacto que hace una copia defensiva de la lista de detalles.
     * Evita que el llamador pueda modificar el contenido del DTO tras la construcción.
     */
    public ReporteResponse {
        detalles = List.copyOf(detalles);
    }
}
