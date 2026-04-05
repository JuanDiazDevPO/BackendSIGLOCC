package com.siglocc.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;

/**
 * Entidad de solo lectura que mapea la vista {@code vista_dashboard_financiero}.
 *
 * <p>Esta vista es el <strong>único Data Source</strong> del Dashboard financiero.
 * Consolida por equipo y temporada los presupuestos calculados desde
 * {@code presupuesto_datos} y {@code parametros_nconnect}, más los anticipos
 * ejecutados de {@code solicitudes_anticipos}.</p>
 *
 * <p><strong>Anotación {@code @Immutable}:</strong> Hibernate no intentará
 * sincronizar cambios a esta entidad con la BD, lo cual es correcto porque
 * es una vista MySQL (no tiene operaciones DML directas).</p>
 *
 * <p><strong>Columnas de jerarquía:</strong> {@code erle_id} y {@code enl_id}
 * son las columnas que usa el repositorio para filtrar por nivel de mando:</p>
 * <ul>
 *   <li>ENL: ve todos los registros de la temporada.</li>
 *   <li>ERLE: ve su propio registro más todos los ERL donde {@code erle_id = suId}.</li>
 *   <li>ERL: ve únicamente su propio registro ({@code equipo_id = suId}).</li>
 * </ul>
 */
@Entity
@Immutable
@Table(name = "vista_dashboard_financiero")
public class VistaDashboardFinanciero {

    /**
     * Clave compuesta formada por {@code equipo_id} y {@code temporada_id}.
     * Identifica unívocamente cada fila de la vista.
     */
    @EmbeddedId
    private VistaDashboardId id;

    /** Nombre descriptivo del equipo (ej: "Atlántico", "Regional Caribe"). */
    @Column(name = "equipo_nombre")
    private String equipoNombre;

    /**
     * Tipo jerárquico del equipo en la estructura de mando.
     * Valores posibles: {@code "ENL"}, {@code "ERLE"}, {@code "ERL"}.
     */
    @Column(name = "equipo_tipo")
    private String equipoTipo;

    /**
     * ID del equipo ERLE que supervisa a este equipo.
     * Solo aplica cuando {@code equipoTipo = "ERL"}.
     * {@code null} para equipos ENL y ERLE.
     */
    @Column(name = "erle_id")
    private Integer erleId;

    /**
     * ID del equipo nacional (ENL).
     * Presente en todos los registros; siempre apunta al equipo raíz.
     */
    @Column(name = "enl_id")
    private Integer enlId;

    /** Presupuesto total asignado al rubro de Entrenamiento en COP. */
    @Column(name = "presupuesto_entrenamiento")
    private BigDecimal presupuestoEntrenamiento;

    /** Monto ya ejecutado (anticipos aprobados) en el rubro de Entrenamiento en COP. */
    @Column(name = "ejecutado_entrenamiento")
    private BigDecimal ejecutadoEntrenamiento;

    /** Saldo disponible en el rubro de Entrenamiento (presupuesto - ejecutado) en COP. */
    @Column(name = "saldo_entrenamiento")
    private BigDecimal saldoEntrenamiento;

    /** Presupuesto total asignado al rubro de Mentoría en COP. */
    @Column(name = "presupuesto_mentoreo")
    private BigDecimal presupuestoMentoreo;

    /** Monto ya ejecutado en el rubro de Mentoría en COP. */
    @Column(name = "ejecutado_mentoreo")
    private BigDecimal ejecutadoMentoreo;

    /** Saldo disponible en el rubro de Mentoría (presupuesto - ejecutado) en COP. */
    @Column(name = "saldo_mentoreo")
    private BigDecimal saldoMentoreo;

    /** Suma de presupuesto Entrenamiento + Mentoría en COP. */
    @Column(name = "gran_total_presupuesto")
    private BigDecimal granTotalPresupuesto;

    /** Suma de ejecutado Entrenamiento + Mentoría en COP. */
    @Column(name = "gran_total_ejecutado")
    private BigDecimal granTotalEjecutado;

    /** Saldo total disponible (gran_total_presupuesto - gran_total_ejecutado) en COP. */
    @Column(name = "gran_total_saldo")
    private BigDecimal granTotalSaldo;

    public VistaDashboardId getId() { return id; }

    public String getEquipoNombre() { return equipoNombre; }

    public String getEquipoTipo() { return equipoTipo; }

    public Integer getErleId() { return erleId; }

    public Integer getEnlId() { return enlId; }

    public BigDecimal getPresupuestoEntrenamiento() { return presupuestoEntrenamiento; }

    public BigDecimal getEjecutadoEntrenamiento() { return ejecutadoEntrenamiento; }

    public BigDecimal getSaldoEntrenamiento() { return saldoEntrenamiento; }

    public BigDecimal getPresupuestoMentoreo() { return presupuestoMentoreo; }

    public BigDecimal getEjecutadoMentoreo() { return ejecutadoMentoreo; }

    public BigDecimal getSaldoMentoreo() { return saldoMentoreo; }

    public BigDecimal getGranTotalPresupuesto() { return granTotalPresupuesto; }

    public BigDecimal getGranTotalEjecutado() { return granTotalEjecutado; }

    public BigDecimal getGranTotalSaldo() { return granTotalSaldo; }
}
