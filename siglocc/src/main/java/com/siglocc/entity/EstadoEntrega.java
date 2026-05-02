package com.siglocc.entity;

/**
 * Estado del acta de entrega de cajas y literatura a una iglesia (Momento 3).
 */
public enum EstadoEntrega {

    /** El acta fue creada pero aún no se ha realizado la entrega física. */
    PENDIENTE,

    /** Toda la cantidad asignada fue entregada a la iglesia. */
    COMPLETADA,

    /** Solo una parte de las cajas pudo ser entregada (entrega parcial). */
    PARCIAL
}
