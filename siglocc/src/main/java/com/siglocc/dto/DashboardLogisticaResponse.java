package com.siglocc.dto;

import java.util.List;

/**
 * Agregados del Dashboard de Logística para una temporada.
 *
 * <p>Todos los conteos vienen ya filtrados por la jerarquía del usuario
 * autenticado (ENL ve el país, ERLE su clúster, ERL su equipo). Se agregan
 * en backend con {@code GROUP BY} en vez de cruzarse en el cliente sobre los
 * 6 endpoints de logística: el payload baja de ~800 KB a ~6 KB.</p>
 *
 * @param temporadaId            temporada consultada
 * @param momentoActual          1=Visión, 2=Capacitación, 3=Entrega — heurística según actividad registrada
 * @param embudoIglesias         conversión del pipeline de iglesias
 * @param embudoCajas            conversión del flujo físico de cajas
 * @param pendientes             contadores de cuellos de botella accionables
 * @param inventarioPorCategoria balance de las 6 categorías de caja, siempre las 6 presentes
 * @param literatura             balance de los 6 tipos de ítem, siempre los 6 presentes
 * @param equipos                avance por equipo visible; vacío si no hay datos
 */
public record DashboardLogisticaResponse(
        Integer temporadaId,
        Integer momentoActual,
        EmbudoIglesiasDto embudoIglesias,
        EmbudoCajasDto embudoCajas,
        PendientesLogisticaDto pendientes,
        List<InventarioCategoriaDto> inventarioPorCategoria,
        List<LiteraturaDto> literatura,
        List<AvanceEquipoDto> equipos
) {
    public DashboardLogisticaResponse {
        inventarioPorCategoria = List.copyOf(inventarioPorCategoria);
        literatura            = List.copyOf(literatura);
        equipos               = List.copyOf(equipos);
    }
}
