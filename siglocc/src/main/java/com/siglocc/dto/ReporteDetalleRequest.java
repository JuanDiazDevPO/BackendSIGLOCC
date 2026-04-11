package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO que representa un rubro de gasto dentro de la solicitud de creación de reporte.
 *
 * <p>El cliente envía una lista de estos objetos en el body de
 * {@code POST /api/v1/reportes}. Cada ítem indica cuánto se gastó en una
 * categoría específica.</p>
 *
 * @param categoriaCodigo código de la categoría de gasto (p. ej. {@code "E-1"}, {@code "M-2"}).
 *                        Debe existir en la tabla {@code reporte_categorias}.
 * @param montoGastado    monto ejecutado en este rubro, en COP. Debe ser mayor o igual a cero.
 */
public record ReporteDetalleRequest(
        String categoriaCodigo,
        BigDecimal montoGastado
) {}
