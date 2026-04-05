package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de anticipo de presupuesto.
 *
 * <p>Mapea la tabla {@code solicitudes_anticipos}. Es el registro central del
 * módulo de anticipos: cada vez que un usuario solicita dinero de una bolsa
 * presupuestal, se crea un registro aquí.</p>
 *
 * <p><strong>Flujo de creación:</strong></p>
 * <ol>
 *   <li>El sistema consulta la vista {@code vista_control_saldos_enl} para
 *       verificar el saldo disponible del equipo en la temporada activa.</li>
 *   <li>Si el monto supera el saldo, se guarda con estado {@code RECHAZADO}
 *       y se completa el campo {@code motivoRechazo} automáticamente.</li>
 *   <li>Si el monto es válido, se guarda con estado {@code PENDIENTE} y
 *       queda en espera de aprobación manual.</li>
 * </ol>
 *
 * <p>Las referencias a usuario, equipo y temporada se guardan como IDs simples
 * (no como relaciones {@code @ManyToOne}) para evitar carga lazy innecesaria.</p>
 */
@Entity
@Table(name = "solicitudes_anticipos")
public class SolicitudAnticipo {

    /** Identificador único autoincremental de la solicitud. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Título corto que describe el propósito de la solicitud. */
    @Column(nullable = false, length = 100)
    private String titulo;

    /** Descripción detallada del gasto que se pretende cubrir. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Monto en COP solicitado. Precisión de 15 dígitos con 2 decimales. */
    @Column(name = "monto_solicitado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoSolicitado;

    /**
     * Rubro presupuestal al que aplica la solicitud.
     * Determina qué saldo se consulta en la vista de control.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_presupuesto", nullable = false)
    private TipoPresupuesto tipoPresupuesto;

    /**
     * Estado actual de la solicitud en su ciclo de vida.
     * Por defecto es {@code PENDIENTE} al momento de la creación.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    /**
     * Razón por la cual la solicitud fue rechazada.
     * Si el rechazo es automático por saldo insuficiente, el sistema
     * rellena este campo con un mensaje estándar.
     */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    /** ID del equipo que realiza la solicitud (tomado de la sesión del usuario). */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada activa al momento de crear la solicitud. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** ID del usuario que creó la solicitud (tomado de la sesión activa). */
    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    /** Fecha y hora en que se creó la solicitud. Se establece automáticamente. */
    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    /**
     * Fecha y hora en que el coordinador aprobó la solicitud.
     * Es {@code null} mientras la solicitud esté en estado {@code PENDIENTE}
     * o {@code RECHAZADO}.
     */
    @Column(name = "fecha_aprobacion_final")
    private LocalDateTime fechaAprobacionFinal;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal montoSolicitado) { this.montoSolicitado = montoSolicitado; }

    public TipoPresupuesto getTipoPresupuesto() { return tipoPresupuesto; }
    public void setTipoPresupuesto(TipoPresupuesto tipoPresupuesto) { this.tipoPresupuesto = tipoPresupuesto; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaAprobacionFinal() { return fechaAprobacionFinal; }
    public void setFechaAprobacionFinal(LocalDateTime fechaAprobacionFinal) { this.fechaAprobacionFinal = fechaAprobacionFinal; }
}
