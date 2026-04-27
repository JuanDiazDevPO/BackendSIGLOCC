package com.siglocc.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con los datos completos de una recepción de contenedor.
 *
 * @param id                    identificador único
 * @param numeroContenedor      número del contenedor
 * @param puntoEntregaId        ID del punto de entrega
 * @param temporadaId           ID de la temporada
 * @param fechaLlegada          fecha de llegada
 * @param totalCajasRecibidas   total de cajas (suma automática de categorías)
 * @param equipoId              ID del equipo que recibió
 * @param observaciones         observaciones
 * @param listaTransportadoraUrl nombre del archivo de lista de transportadora
 * @param documentoAbcUrl       nombre del archivo ABC
 * @param fechaRegistro         fecha y hora de creación del registro
 * @param detalles              desglose por categoría/ítem con cantidades
 */
public record RecepcionContenedorResponse(
        Integer id,
        String numeroContenedor,
        Integer puntoEntregaId,
        Integer temporadaId,
        LocalDate fechaLlegada,
        Integer totalCajasRecibidas,
        Integer equipoId,
        String observaciones,
        String listaTransportadoraUrl,
        String documentoAbcUrl,
        LocalDateTime fechaRegistro,
        List<DetalleRecepcionResponse> detalles
) {
    public RecepcionContenedorResponse {
        detalles = List.copyOf(detalles != null ? detalles : List.of());
    }
}
