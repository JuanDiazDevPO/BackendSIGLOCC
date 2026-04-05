package com.siglocc.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;

/**
 * Entidad de solo lectura que mapea la vista {@code vista_control_saldos_enl}.
 *
 * <p>Esta vista es el componente central del módulo de anticipos. Consolida
 * el presupuesto total asignado y el monto ya ejecutado (anticipos aprobados)
 * por equipo y temporada, permitiendo calcular el saldo disponible en tiempo
 * real antes de aceptar una solicitud.</p>
 *
 * <p><strong>Lógica de saldo:</strong></p>
 * <pre>
 *   Saldo ENTRENAMIENTO = presupuesto_entrenamiento - ejecutado_entrenamiento
 *   Saldo MENTOREO      = presupuesto_mentoreo      - ejecutado_mentoreo
 * </pre>
 *
 * <p>{@code @Immutable} le indica a Hibernate que nunca intente hacer
 * {@code INSERT}, {@code UPDATE} o {@code DELETE} sobre esta entidad,
 * ya que es una vista de BD y no una tabla.</p>
 *
 * <p>La clave primaria es compuesta: ver {@link VistaControlSaldosId}.</p>
 */
@Entity
@Immutable
@Table(name = "vista_control_saldos_enl")
public class VistaControlSaldos {

    /** Clave compuesta: (equipo_id, temporada_id). */
    @EmbeddedId
    private VistaControlSaldosId id;

    /** Presupuesto total asignado al rubro de entrenamiento en COP. */
    @Column(name = "presupuesto_entrenamiento", precision = 15, scale = 2)
    private BigDecimal presupuestoEntrenamiento;

    /** Monto ya ejecutado (anticipos aprobados) del rubro de entrenamiento en COP. */
    @Column(name = "ejecutado_entrenamiento", precision = 15, scale = 2)
    private BigDecimal ejecutadoEntrenamiento;

    /** Presupuesto total asignado al rubro de mentoreo en COP. */
    @Column(name = "presupuesto_mentoreo", precision = 15, scale = 2)
    private BigDecimal presupuestoMentoreo;

    /** Monto ya ejecutado (anticipos aprobados) del rubro de mentoreo en COP. */
    @Column(name = "ejecutado_mentoreo", precision = 15, scale = 2)
    private BigDecimal ejecutadoMentoreo;

    public VistaControlSaldosId getId() { return id; }

    /**
     * Calcula el saldo disponible para entrenamiento.
     * @return presupuesto_entrenamiento - ejecutado_entrenamiento
     */
    public BigDecimal getSaldoEntrenamiento() {
        return presupuestoEntrenamiento.subtract(ejecutadoEntrenamiento);
    }

    /**
     * Calcula el saldo disponible para mentoreo.
     * @return presupuesto_mentoreo - ejecutado_mentoreo
     */
    public BigDecimal getSaldoMentoreo() {
        return presupuestoMentoreo.subtract(ejecutadoMentoreo);
    }

    public BigDecimal getPresupuestoEntrenamiento() { return presupuestoEntrenamiento; }
    public BigDecimal getEjecutadoEntrenamiento() { return ejecutadoEntrenamiento; }
    public BigDecimal getPresupuestoMentoreo() { return presupuestoMentoreo; }
    public BigDecimal getEjecutadoMentoreo() { return ejecutadoMentoreo; }
}
