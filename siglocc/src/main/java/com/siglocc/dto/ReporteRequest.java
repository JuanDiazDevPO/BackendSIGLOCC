package com.siglocc.dto;

import java.util.List;

/**
 * DTO para la creación de un reporte mensual de gastos.
 *
 * <p>Se envía en el body de {@code POST /api/v1/reportes}. El {@code equipoId}
 * <strong>no</strong> se incluye aquí; el servicio lo extrae directamente del
 * token JWT para evitar que un usuario reporte a nombre de otro equipo.</p>
 *
 * <p>Ejemplo de request:</p>
 * <pre>{@code
 * {
 *   "temporadaId": 1,
 *   "mes": 3,
 *   "anio": 2025,
 *   "detalles": [
 *     { "categoriaCodigo": "E-1", "montoGastado": 500000.00 },
 *     { "categoriaCodigo": "E-2", "montoGastado": 300000.00 }
 *   ]
 * }
 * }</pre>
 *
 * @param temporadaId ID de la temporada a la que pertenece el reporte.
 * @param mes         mes del período reportado (1 = enero … 12 = diciembre).
 * @param anio        año del período reportado.
 * @param detalles    lista de rubros con sus montos ejecutados.
 */
public record ReporteRequest(
        Integer temporadaId,
        Integer mes,
        Integer anio,
        List<ReporteDetalleRequest> detalles
) {
    /**
     * Constructor compacto que hace una copia defensiva de la lista de detalles.
     * Evita que el llamador pueda modificar el contenido del DTO tras la construcción.
     */
    public ReporteRequest {
        detalles = List.copyOf(detalles);
    }
}
