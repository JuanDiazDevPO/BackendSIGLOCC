package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de entrada para actualizar únicamente la tasa de cambio de una temporada.
 *
 * <p>Al actualizar la TRM con este endpoint, <strong>todos los saldos del país
 * se recalculan automáticamente</strong> en la próxima consulta a las vistas,
 * sin necesidad de modificar ningún otro dato.</p>
 *
 * @param tasaCambio Nueva TRM en COP/USD (ej: 4150.00)
 */
public record ActualizarTasaCambioRequest(
        BigDecimal tasaCambio
) {}
