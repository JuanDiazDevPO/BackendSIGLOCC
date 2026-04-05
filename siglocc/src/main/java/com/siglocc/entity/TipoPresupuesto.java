package com.siglocc.entity;

/**
 * Enumeración que representa los dos grandes rubros o "bolsas" de presupuesto
 * que maneja el sistema SIGLOCC por equipo.
 *
 * <ul>
 *   <li>{@code ENTRENAMIENTO} – Suma del presupuesto de Visión, Capacitación
 *       y Oración. Se valida contra la columna {@code presupuesto_entrenamiento}
 *       de la vista {@code vista_control_saldos_enl}.</li>
 *   <li>{@code MENTOREO} – Cálculo basado en visitas y personas.
 *       Se valida contra la columna {@code presupuesto_mentoreo}
 *       de la misma vista.</li>
 * </ul>
 *
 * <p>Se almacena como {@code ENUM('ENTRENAMIENTO','MENTOREO')} en la columna
 * {@code tipo_presupuesto} de la tabla {@code solicitudes_anticipos}.</p>
 */
public enum TipoPresupuesto {
    ENTRENAMIENTO, MENTOREO
}
