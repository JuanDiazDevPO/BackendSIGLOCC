package com.siglocc.entity;

/**
 * Estado de la solicitud de participación de una iglesia en la operación logística.
 *
 * <p>Una iglesia pasa por este flujo tras inscribirse en el Momento 1 (La Visión):</p>
 * <pre>
 * PENDIENTE → APROBADA
 *          → RECHAZADA
 * </pre>
 * <p>Solo las iglesias en estado {@link #APROBADA} participan en el Momento 2
 * (Capacitación) y el Momento 3 (Entrega).</p>
 */
public enum EstadoIglesia {

    /** La iglesia se ha inscrito y está esperando decisión del coordinador. */
    PENDIENTE,

    /** La iglesia fue aceptada y puede recibir cajas y literatura. */
    APROBADA,

    /** La iglesia fue rechazada; el campo {@code motivoRechazo} explica la razón. */
    RECHAZADA
}
