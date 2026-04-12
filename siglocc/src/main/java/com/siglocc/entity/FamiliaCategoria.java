package com.siglocc.entity;

/**
 * Enumera las familias a las que puede pertenecer una categoría de gasto.
 *
 * <p>Esta clasificación determina:</p>
 * <ul>
 *   <li>Qué tipo de gasto representa el rubro (entrenamiento, mentoría u otro).</li>
 *   <li>Qué equipos pueden reportar en cada familia:
 *       los equipos <strong>ERL</strong> solo tienen acceso a la familia {@link #E},
 *       mientras que <strong>ERLE</strong> y <strong>ENL</strong> pueden reportar
 *       en las tres.</li>
 * </ul>
 */
public enum FamiliaCategoria {

    /** Entrenamiento: rubros asociados a actividades de formación y capacitación. */
    E,

    /** Mentoría: rubros asociados al proceso de acompañamiento entre equipos. */
    M,

    /** Otros: rubros misceláneos que no encajan en las familias E ni M. */
    O
}
