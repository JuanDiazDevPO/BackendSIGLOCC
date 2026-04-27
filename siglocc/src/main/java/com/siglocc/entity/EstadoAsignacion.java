package com.siglocc.entity;

/**
 * Estado de una corrida de asignación inteligente.
 *
 * <ul>
 *   <li>{@link #BORRADOR} – generada, visible para revisión y ajuste manual.</li>
 *   <li>{@link #CONFIRMADA} – aprobada por el coordinador; descuenta el inventario.</li>
 * </ul>
 */
public enum EstadoAsignacion {
    BORRADOR,
    CONFIRMADA
}
