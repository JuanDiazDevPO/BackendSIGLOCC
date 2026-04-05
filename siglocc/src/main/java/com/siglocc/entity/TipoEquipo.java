package com.siglocc.entity;

/**
 * Enumeración que representa los tipos de equipo dentro del sistema SIGLOCC.
 *
 * <p>La jerarquía de equipos es la siguiente:</p>
 * <ul>
 *   <li>{@code ENL} – Equipo Nacional de Liderazgo. Es el nivel más alto.
 *       Su {@code enl_id} y {@code erle_id} son {@code null}.</li>
 *   <li>{@code ERLE} – Equipo Regional de Liderazgo Extendido. Reporta al ENL.
 *       Tiene {@code enl_id} apuntando al equipo ENL.</li>
 *   <li>{@code ERL} – Equipo Regional de Liderazgo. Reporta a un ERLE.
 *       Tiene {@code erle_id} apuntando al equipo ERLE.</li>
 * </ul>
 *
 * <p>Se almacena como {@code ENUM('ENL','ERLE','ERL')} en la columna
 * {@code tipo} de la tabla {@code equipos}.</p>
 */
public enum TipoEquipo {
    ENL, ERLE, ERL
}
