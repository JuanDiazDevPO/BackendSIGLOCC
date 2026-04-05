package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa los datos operativos de presupuesto ingresados por un equipo.
 *
 * <p>Mapea la tabla {@code presupuesto_datos}. Almacena los <strong>inputs manuales</strong>
 * que cada equipo planea ejecutar en la temporada: cuántas presentaciones de visión,
 * cuántos entrenadores, cuántos invitados, etc.</p>
 *
 * <p><strong>Flujo de uso:</strong></p>
 * <ol>
 *   <li>El ENL configura {@link ParametrosNconnect} para la temporada (prerequisito).</li>
 *   <li>Cada equipo debe tener su meta de contenedores en {@code metas_equipo}.</li>
 *   <li>El equipo ingresa sus datos operativos aquí (manual o por CSV).</li>
 *   <li>La vista de presupuesto en BD toma estos inputs y los parámetros ENL
 *       para calcular el presupuesto total en COP automáticamente.</li>
 * </ol>
 *
 * <p>Las referencias a equipo y temporada se guardan como IDs simples
 * (no {@code @ManyToOne}) para evitar carga lazy innecesaria.</p>
 */
@Entity
@Table(name = "presupuesto_datos")
public class PresupuestoDatos {

    /** Identificador único autoincremental del registro de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID del equipo al que pertenecen estos datos. */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada para la que se está ingresando el presupuesto. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /**
     * Promedio de cajas por Punto de Venta o Contenedor.
     * Es el indicador base para calcular LGA.
     */
    @Column(name = "promedio_cm_cont", nullable = false, precision = 10, scale = 2)
    private BigDecimal promedioCmCont;

    /** Número de Presentaciones de Visión (eventos) planificados. */
    @Column(name = "num_pv", nullable = false)
    private Integer numPv;

    /** Cantidad de entrenadores (staff) por cada Presentación de Visión. */
    @Column(name = "entrenadores_pv", nullable = false)
    private Integer entrenadoresPv;

    /** Cantidad de personas invitadas por cada Presentación de Visión. */
    @Column(name = "personas_pv", nullable = false)
    private Integer personasPv;

    /**
     * Número de maestros LGA.
     * Se ingresa manualmente y no se calcula automáticamente.
     */
    @Column(name = "maestros_lga", nullable = false)
    private Integer maestrosLga;

    /** Número de Capacitaciones OCC planificadas en la temporada. */
    @Column(name = "num_cap_occ", nullable = false)
    private Integer numCapOcc;

    /** Cantidad de entrenadores por cada Capacitación OCC. */
    @Column(name = "num_entrenadores_cap", nullable = false)
    private Integer numEntrenadoresCap;

    /**
     * Número de equipos bajo mentoría directa.
     * Es el multiplicador clave para el cálculo del presupuesto de mentoría:
     * {@code equiposBajoMentoreo × visitas × personasPorVisita}.
     */
    @Column(name = "equipos_bajo_mentoreo", nullable = false)
    private Integer equiposBajoMentoreo;

    /** Monto destinado a oración, ingresado directamente en COP. */
    @Column(name = "monto_oracion_cop", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoOracionCop;

    /** Fecha y hora en que se registraron estos datos. Se establece automáticamente. */
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public BigDecimal getPromedioCmCont() { return promedioCmCont; }
    public void setPromedioCmCont(BigDecimal promedioCmCont) { this.promedioCmCont = promedioCmCont; }

    public Integer getNumPv() { return numPv; }
    public void setNumPv(Integer numPv) { this.numPv = numPv; }

    public Integer getEntrenadoresPv() { return entrenadoresPv; }
    public void setEntrenadoresPv(Integer entrenadoresPv) { this.entrenadoresPv = entrenadoresPv; }

    public Integer getPersonasPv() { return personasPv; }
    public void setPersonasPv(Integer personasPv) { this.personasPv = personasPv; }

    public Integer getMaestrosLga() { return maestrosLga; }
    public void setMaestrosLga(Integer maestrosLga) { this.maestrosLga = maestrosLga; }

    public Integer getNumCapOcc() { return numCapOcc; }
    public void setNumCapOcc(Integer numCapOcc) { this.numCapOcc = numCapOcc; }

    public Integer getNumEntrenadoresCap() { return numEntrenadoresCap; }
    public void setNumEntrenadoresCap(Integer numEntrenadoresCap) { this.numEntrenadoresCap = numEntrenadoresCap; }

    public Integer getEquiposBajoMentoreo() { return equiposBajoMentoreo; }
    public void setEquiposBajoMentoreo(Integer equiposBajoMentoreo) { this.equiposBajoMentoreo = equiposBajoMentoreo; }

    public BigDecimal getMontoOracionCop() { return montoOracionCop; }
    public void setMontoOracionCop(BigDecimal montoOracionCop) { this.montoOracionCop = montoOracionCop; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
