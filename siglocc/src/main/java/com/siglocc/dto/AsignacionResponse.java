package com.siglocc.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta completa de una corrida de asignación inteligente.
 *
 * <p>Incluye el encabezado con los snapshots de inventario al momento de
 * generar, y todas las líneas de asignación por iglesia × categoría/ítem.</p>
 *
 * @param id                     identificador único de la cabecera
 * @param equipoId               equipo que generó la asignación
 * @param temporadaId            temporada
 * @param fechaGeneracion        cuándo se generó
 * @param generadaAutomaticamente true si fue el motor Hamilton, false si fue manual
 * @param estado                 BORRADOR o CONFIRMADA
 * @param totalCajasDisponibles  snapshot: total de cajas al generar
 * @param totalCajasSolicitadas  snapshot: total pedido por iglesias aprobadas
 * @param factorReduccion        1.0 si alcanza; < 1.0 si hubo reducción proporcional
 * @param observaciones          notas del coordinador
 * @param detalles               líneas por iglesia × categoría/ítem
 */
public record AsignacionResponse(
        Integer id,
        Integer equipoId,
        Integer temporadaId,
        LocalDateTime fechaGeneracion,
        Boolean generadaAutomaticamente,
        String estado,
        Integer totalCajasDisponibles,
        Integer totalCajasSolicitadas,
        BigDecimal factorReduccion,
        String observaciones,
        List<AsignacionDetalleResponse> detalles
) {
    public AsignacionResponse {
        detalles = List.copyOf(detalles != null ? detalles : List.of());
    }
}
