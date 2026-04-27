package com.siglocc.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con los datos completos de un acta de entrega a una iglesia.
 *
 * @param id             identificador único del acta
 * @param iglesiaId      ID de la iglesia
 * @param puntoEntregaId ID del punto de entrega
 * @param temporadaId    ID de la temporada
 * @param equipoId       ID del equipo coordinador
 * @param fechaEntrega   fecha de la entrega física
 * @param firmaTipo      tipo de firma (DIGITAL o ESCANEADA)
 * @param firmaUrl       nombre del archivo de firma (puede ser null)
 * @param estado         estado del acta (PENDIENTE, COMPLETADA, PARCIAL)
 * @param observaciones  observaciones de la entrega
 * @param confirmado     si el coordinador confirmó la recepción
 * @param fechaRegistro  fecha y hora de creación del acta
 * @param detalles       ítems entregados con sus cantidades
 */
public record EntregaResponse(
        Integer id,
        Integer iglesiaId,
        Integer puntoEntregaId,
        Integer temporadaId,
        Integer equipoId,
        LocalDate fechaEntrega,
        String firmaTipo,
        String firmaUrl,
        String estado,
        String observaciones,
        Boolean confirmado,
        LocalDateTime fechaRegistro,
        List<EntregaDetalleResponse> detalles
) {
    public EntregaResponse {
        detalles = List.copyOf(detalles != null ? detalles : List.of());
    }
}
