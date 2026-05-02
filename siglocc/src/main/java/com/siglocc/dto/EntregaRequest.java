package com.siglocc.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos para crear el acta de entrega de cajas y literatura a una iglesia (Momento 3).
 *
 * <p>El {@code equipoId} se extrae del JWT. La firma se sube por un endpoint separado
 * ({@code PUT /entregas/{id}/firma}) después de crear el acta.</p>
 *
 * @param iglesiaId     ID de la iglesia que recibe el material
 * @param puntoEntregaId ID del punto de entrega donde se realiza la distribución
 * @param temporadaId   ID de la temporada
 * @param fechaEntrega  fecha en que se realizó la entrega física
 * @param firmaTipo     tipo de firma: «DIGITAL» o «ESCANEADA»
 * @param observaciones observaciones sobre la entrega
 * @param detalles      lista de ítems y cantidades entregadas
 */
public record EntregaRequest(
        Integer iglesiaId,
        Integer puntoEntregaId,
        Integer temporadaId,
        LocalDate fechaEntrega,
        String firmaTipo,
        String observaciones,
        List<EntregaDetalleRequest> detalles
) {
    public EntregaRequest {
        detalles = List.copyOf(detalles != null ? detalles : List.of());
    }
}
