package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta que representa un rubro individual dentro de un reporte mensual.
 *
 * <p>Se incluye en la lista {@code detalles} de {@link ReporteResponse}.
 * Agrega el nombre completo y la familia de la categoría para facilitar
 * la visualización en el front-end sin necesidad de una segunda consulta.</p>
 *
 * @param categoriaCodigo código corto de la categoría (p. ej. {@code "E-1"}).
 * @param nombreCategoria descripción larga de la categoría (p. ej. {@code "Refrigerios punto de venta"}).
 * @param familia         familia a la que pertenece ({@code "E"}, {@code "M"} u {@code "O"}).
 * @param montoGastado    monto ejecutado en este rubro, en COP.
 */
public record ReporteDetalleResponse(
        String categoriaCodigo,
        String nombreCategoria,
        String familia,
        BigDecimal montoGastado
) {}
