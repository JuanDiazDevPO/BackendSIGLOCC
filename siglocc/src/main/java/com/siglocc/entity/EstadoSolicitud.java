package com.siglocc.entity;

/**
 * Enumeración que representa el estado del ciclo de vida de una solicitud
 * de anticipo en el sistema SIGLOCC.
 *
 * <p>Flujo de estados:</p>
 * <pre>
 *  (nueva solicitud)
 *        │
 *        ▼
 *    PENDIENTE ──── (ENL_RECURSOS aprueba) ────► APROBADO
 *        │
 *        └──── (sistema detecta saldo insuficiente) ──► RECHAZADO
 * </pre>
 *
 * <ul>
 *   <li>{@code PENDIENTE} – La solicitud pasó la validación automática de saldo
 *       y espera la aprobación manual del coordinador ENL_RECURSOS.</li>
 *   <li>{@code APROBADO} – El coordinador aprobó la solicitud. El monto queda
 *       registrado como ejecutado en la vista de saldos.</li>
 *   <li>{@code RECHAZADO} – El sistema rechazó automáticamente la solicitud
 *       porque el monto supera el saldo disponible. El campo
 *       {@code motivo_rechazo} de la entidad {@link SolicitudAnticipo}
 *       contiene la razón.</li>
 * </ul>
 */
public enum EstadoSolicitud {
    PENDIENTE, APROBADO, RECHAZADO
}
