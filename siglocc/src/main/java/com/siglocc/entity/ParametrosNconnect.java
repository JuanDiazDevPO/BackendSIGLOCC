package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad que representa los parámetros financieros globales del módulo ENL (Nconnect).
 *
 * <p>Mapea la tabla {@code parametros_nconnect}. Es el <strong>corazón financiero</strong>
 * del motor de presupuestos: el coordinador ENL ingresa aquí todos los costos unitarios
 * en dólares y los factores de conversión que la vista de presupuesto usará para calcular
 * los valores en COP de cada equipo.</p>
 *
 * <p><strong>Regla de negocio crítica:</strong> Debe existir exactamente un registro
 * por temporada ({@code temporada_id UNIQUE}). Si no existe un registro para la
 * temporada activa, los endpoints de presupuesto deben estar bloqueados.</p>
 *
 * <p><strong>Efecto de actualizar la tasa de cambio:</strong> Al ejecutar un
 * {@code UPDATE parametros_nconnect SET tasa_cambio = x WHERE temporada_id = y},
 * automáticamente todas las vistas de saldos de todos los equipos reflejan los
 * nuevos valores en COP sin tocar una sola línea de código Java.</p>
 */
@Entity
@Table(name = "parametros_nconnect")
public class ParametrosNconnect {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID de la temporada a la que aplican estos parámetros.
     * Restricción UNIQUE: solo puede existir un registro por temporada.
     */
    @Column(name = "temporada_id", nullable = false, unique = true)
    private Integer temporadaId;

    /** TRM (Tasa Representativa del Mercado) pactada para la temporada en COP/USD. */
    @Column(name = "tasa_cambio", nullable = false, precision = 10, scale = 2)
    private BigDecimal tasaCambio;

    /** Estándar de cajas por contenedor (ej: 7368). Usado para calcular metas de LGA. */
    @Column(name = "cajas_por_contenedor", nullable = false)
    private Integer cajasPorContenedor;

    /** Factor de eficiencia LGA en decimal (ej: 0.65 = 65%). */
    @Column(name = "porcentaje_lga", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeLga;

    /** Costo unitario de administración CM en USD. */
    @Column(name = "usd_admin_cm", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdAdminCm;

    /** Costo de refrigerio por persona en Presentación de Visión (PV) en USD. */
    @Column(name = "usd_refrigerio_pv", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdRefrigeroPv;

    /** Costo de transporte por persona en PV en USD. */
    @Column(name = "usd_transporte_pv", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdTransportePv;

    /** Costo de transporte por persona en Capacitación OCC en USD. */
    @Column(name = "usd_transporte_cap", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdTransporteCap;

    /** Costo de refrigerio por persona en Capacitación OCC en USD. */
    @Column(name = "usd_refrigerio_cap", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdRefrierioCap;

    /**
     * Número de visitas de mentoría por equipo por temporada.
     * Fijo en 3 por definición del proceso.
     */
    @Column(name = "visitas_mentoreo", nullable = false)
    private Integer visitasMentoreo;

    /**
     * Personas que asisten por visita de mentoría.
     * Fijo en 2 por definición del proceso.
     */
    @Column(name = "personas_por_visita", nullable = false)
    private Integer personasPorVisita;

    /** Costo de transporte por persona en visita de mentoría en USD. */
    @Column(name = "usd_transporte_mentoreo", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdTransporteMentoreo;

    /** Costo de alimentación por persona en visita de mentoría en USD. */
    @Column(name = "usd_alimento_mentoreo", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdAlimentoMentoreo;

    /** Costo de hospedaje por persona en visita de mentoría en USD. */
    @Column(name = "usd_hospedaje_mentoreo", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdHospedajeMentoreo;

    /** Costo de administración por visita de mentoría en USD. */
    @Column(name = "usd_admin_mentoreo", nullable = false, precision = 10, scale = 4)
    private BigDecimal usdAdminMentoreo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public BigDecimal getTasaCambio() { return tasaCambio; }
    public void setTasaCambio(BigDecimal tasaCambio) { this.tasaCambio = tasaCambio; }

    public Integer getCajasPorContenedor() { return cajasPorContenedor; }
    public void setCajasPorContenedor(Integer cajasPorContenedor) { this.cajasPorContenedor = cajasPorContenedor; }

    public BigDecimal getPorcentajeLga() { return porcentajeLga; }
    public void setPorcentajeLga(BigDecimal porcentajeLga) { this.porcentajeLga = porcentajeLga; }

    public BigDecimal getUsdAdminCm() { return usdAdminCm; }
    public void setUsdAdminCm(BigDecimal usdAdminCm) { this.usdAdminCm = usdAdminCm; }

    public BigDecimal getUsdRefrigeroPv() { return usdRefrigeroPv; }
    public void setUsdRefrigeroPv(BigDecimal usdRefrigeroPv) { this.usdRefrigeroPv = usdRefrigeroPv; }

    public BigDecimal getUsdTransportePv() { return usdTransportePv; }
    public void setUsdTransportePv(BigDecimal usdTransportePv) { this.usdTransportePv = usdTransportePv; }

    public BigDecimal getUsdTransporteCap() { return usdTransporteCap; }
    public void setUsdTransporteCap(BigDecimal usdTransporteCap) { this.usdTransporteCap = usdTransporteCap; }

    public BigDecimal getUsdRefrierioCap() { return usdRefrierioCap; }
    public void setUsdRefrierioCap(BigDecimal usdRefrierioCap) { this.usdRefrierioCap = usdRefrierioCap; }

    public Integer getVisitasMentoreo() { return visitasMentoreo; }
    public void setVisitasMentoreo(Integer visitasMentoreo) { this.visitasMentoreo = visitasMentoreo; }

    public Integer getPersonasPorVisita() { return personasPorVisita; }
    public void setPersonasPorVisita(Integer personasPorVisita) { this.personasPorVisita = personasPorVisita; }

    public BigDecimal getUsdTransporteMentoreo() { return usdTransporteMentoreo; }
    public void setUsdTransporteMentoreo(BigDecimal usdTransporteMentoreo) { this.usdTransporteMentoreo = usdTransporteMentoreo; }

    public BigDecimal getUsdAlimentoMentoreo() { return usdAlimentoMentoreo; }
    public void setUsdAlimentoMentoreo(BigDecimal usdAlimentoMentoreo) { this.usdAlimentoMentoreo = usdAlimentoMentoreo; }

    public BigDecimal getUsdHospedajeMentoreo() { return usdHospedajeMentoreo; }
    public void setUsdHospedajeMentoreo(BigDecimal usdHospedajeMentoreo) { this.usdHospedajeMentoreo = usdHospedajeMentoreo; }

    public BigDecimal getUsdAdminMentoreo() { return usdAdminMentoreo; }
    public void setUsdAdminMentoreo(BigDecimal usdAdminMentoreo) { this.usdAdminMentoreo = usdAdminMentoreo; }
}
