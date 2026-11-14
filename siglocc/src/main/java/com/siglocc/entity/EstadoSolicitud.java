package com.siglocc.entity;

/**
 * Enumeración que representa el estado del ciclo de vida de una solicitud
 * de anticipo en el sistema SIGLOCC.
 *
 * <p>El flujo de aprobación depende del tipo de equipo que solicita, igual
 * que en {@link EstadoReporte}:</p>
 * <pre>
 *  Equipo ERL:
 *    (nueva solicitud) ──▶ PENDIENTE_ERLE ──(ERLE aprueba)──▶ PENDIENTE_ENL ──(ENL aprueba)──▶ APROBADO
 *                                │ (ERLE rechaza)                   │ (ENL rechaza)
 *                                ▼                                  ▼
 *                           RECHAZADO                           RECHAZADO
 *
 *  Equipo ERLE:
 *    (nueva solicitud) ──▶ PENDIENTE_ENL ──(ENL aprueba)──▶ APROBADO
 *                                │ (ENL rechaza)
 *                                ▼
 *                           RECHAZADO
 *
 *  Equipo ENL:
 *    (nueva solicitud) ──▶ APROBADO   (autoaprobada; no hay nadie por encima)
 * </pre>
 *
 * <p>En cualquier punto del flujo, el sistema puede rechazar automáticamente
 * la solicitud antes de que exista aprobación manual si el monto supera el
 * saldo disponible (ver {@link SolicitudAnticipo#getMotivoRechazo()}).</p>
 *
 * <ul>
 *   <li>{@code PENDIENTE_ERLE} – Solicitud de un equipo ERL en espera de la
 *       primera revisión de su ERLE.</li>
 *   <li>{@code PENDIENTE_ENL} – Aprobada por el ERLE (o creada directamente
 *       por un equipo ERLE), en espera de la aprobación final del ENL.</li>
 *   <li>{@code APROBADO} – Aprobación final. El monto queda confirmado.</li>
 *   <li>{@code RECHAZADO} – Rechazada en cualquier punto del flujo, ya sea
 *       automáticamente por el sistema o manualmente por el ERLE/ENL.</li>
 * </ul>
 */
public enum EstadoSolicitud {
    PENDIENTE_ERLE, PENDIENTE_ENL, APROBADO, RECHAZADO
}
