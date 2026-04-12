package com.siglocc.entity;

/**
 * Enumera los posibles estados de un reporte mensual a lo largo de su ciclo de vida.
 *
 * <p>El flujo estándar de transición es:</p>
 * <pre>
 *   BORRADOR
 *      │  (ERL adjunta soporte vía PUT /soporte)
 *      ▼
 *   PENDIENTE_ERLE
 *      │  (ERLE aprueba)           (ERLE rechaza)
 *      ▼                                ▼
 *   PENDIENTE_ENL               RECHAZADO
 *      │  (ENL aprueba)            (ENL rechaza)
 *      ▼                                ▼
 *   APROBADO                    RECHAZADO
 * </pre>
 *
 * <p>Solo el estado {@link #APROBADO} activa la actualización financiera en las
 * vistas MySQL del sistema de control de saldos.</p>
 */
public enum EstadoReporte {

    /** Reporte creado pero sin soporte adjunto; aún no ha ingresado al flujo de aprobación. */
    BORRADOR,

    /** Soporte adjunto; en espera de revisión del ERLE responsable del clúster. */
    PENDIENTE_ERLE,

    /** Aprobado por el ERLE; en espera de aprobación final del ENL. */
    PENDIENTE_ENL,

    /** Aprobado por el ENL. Los montos se suman al ejecutado en las vistas financieras. */
    APROBADO,

    /** Rechazado en cualquier punto del flujo. El campo {@code observaciones} contiene el motivo. */
    RECHAZADO
}
