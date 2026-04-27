package com.siglocc.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos para registrar la llegada de un contenedor a un punto de entrega.
 *
 * <p>El {@code equipoId} se extrae del JWT. El campo {@code totalCajasRecibidas}
 * se calcula automáticamente como la suma de las cantidades de los detalles de
 * categorías de caja; no es necesario enviarlo.</p>
 *
 * <p>Los documentos adjuntos (lista transportadora, documento ABC) se suben
 * por endpoints separados tras crear el registro.</p>
 *
 * @param numeroContenedor número identificador del contenedor (en la puerta)
 * @param puntoEntregaId   ID del punto de entrega donde llega
 * @param temporadaId      ID de la temporada
 * @param fechaLlegada     fecha de llegada del contenedor
 * @param observaciones    observaciones sobre el estado del contenedor
 * @param detalles         desglose por categoría de caja y literatura recibida
 */
public record RecepcionContenedorRequest(
        String numeroContenedor,
        Integer puntoEntregaId,
        Integer temporadaId,
        LocalDate fechaLlegada,
        String observaciones,
        List<DetalleRecepcionRequest> detalles
) {
    public RecepcionContenedorRequest {
        detalles = List.copyOf(detalles != null ? detalles : List.of());
    }
}
