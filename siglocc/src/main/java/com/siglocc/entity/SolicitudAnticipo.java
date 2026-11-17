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
 *   <li>En ambos casos se genera un PDF formal de solicitud y se almacena
 *       localmente; la ruta queda en {@code rutaPdf}.</li>
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

    /** Descripción detallada del gasto / destino del anticipo. */
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
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE_ERLE;

    /**
     * Razón por la cual la solicitud fue rechazada.
     * Si el rechazo es automático por saldo insuficiente, el sistema
     * rellena este campo con un mensaje estándar; si es manual, contiene
     * las observaciones del ERLE o ENL que rechazó.
     */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    // ── Datos del solicitante ───────────────────────────────────────────────

    /** Ciudad desde donde se realiza la solicitud (ej: BUCARAMANGA). */
    @Column(length = 80)
    private String ciudad;

    /** Cédula de ciudadanía del solicitante. */
    @Column(length = 20)
    private String cedula;

    // ── Datos bancarios para el giro ────────────────────────────────────────

    /** Nombre del banco destino (ej: NEQUI, BANCOLOMBIA). */
    @Column(length = 60)
    private String banco;

    /** Tipo de cuenta bancaria del beneficiario. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuenta")
    private TipoCuenta tipoCuenta;

    /** Número de cuenta bancaria del beneficiario. */
    @Column(name = "numero_cuenta", length = 30)
    private String numeroCuenta;

    /** Nombre completo del titular de la cuenta bancaria. */
    @Column(name = "nombre_titular", length = 100)
    private String nombreTitular;

    /** Cédula del titular de la cuenta bancaria. */
    @Column(name = "cedula_titular", length = 20)
    private String cedulaTitular;

    // ── Ruta del documento PDF generado ────────────────────────────────────

    /**
     * Ruta relativa del PDF formal generado al crear la solicitud.
     * Ejemplo: {@code anticipos/ANTICIPO_12.pdf}
     */
    @Column(name = "ruta_pdf", length = 200)
    private String rutaPdf;

    // ── IDs de contexto ─────────────────────────────────────────────────────

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
     * Fecha y hora en que el ENL dio la aprobación final a la solicitud
     * (o en que quedó autoaprobada, si la solicitó el propio equipo ENL).
     * Es {@code null} mientras la solicitud no esté en estado {@code APROBADO}.
     */
    @Column(name = "fecha_aprobacion_final")
    private LocalDateTime fechaAprobacionFinal;

    /**
     * ID del usuario ERLE que dio el primer visto bueno a la solicitud.
     * Se registra cuando el estado transiciona de {@code PENDIENTE_ERLE}
     * a {@code PENDIENTE_ENL}. Es {@code null} hasta ese momento, y también
     * cuando la solicitud la hace directamente un equipo ERLE o ENL.
     */
    @Column(name = "aprobador_erle_id")
    private Integer aprobadorErleId;

    /** Fecha y hora en que el ERLE aprobó el primer paso. Es {@code null} hasta ese momento. */
    @Column(name = "fecha_aprobacion_erle")
    private LocalDateTime fechaAprobacionErle;

    /**
     * ID del usuario ENL que cerró el proceso de aprobación (aprobando o
     * rechazando), o que generó la solicitud si el propio equipo ENL la
     * autoaprobó. Es {@code null} hasta que el ENL actúa sobre la solicitud.
     */
    @Column(name = "aprobador_enl_id")
    private Integer aprobadorEnlId;

    /**
     * ID del usuario (ERLE o ENL) que rechazó manualmente la solicitud.
     * Es {@code null} si la solicitud no ha sido rechazada, o si el rechazo
     * fue automático por saldo insuficiente.
     */
    @Column(name = "rechazado_por_id")
    private Integer rechazadoPorId;

    /** Fecha y hora del rechazo, automático o manual. Es {@code null} si no aplica. */
    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

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

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public TipoCuenta getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(TipoCuenta tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public String getNombreTitular() { return nombreTitular; }
    public void setNombreTitular(String nombreTitular) { this.nombreTitular = nombreTitular; }

    public String getCedulaTitular() { return cedulaTitular; }
    public void setCedulaTitular(String cedulaTitular) { this.cedulaTitular = cedulaTitular; }

    public String getRutaPdf() { return rutaPdf; }
    public void setRutaPdf(String rutaPdf) { this.rutaPdf = rutaPdf; }

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

    public Integer getAprobadorErleId() { return aprobadorErleId; }
    public void setAprobadorErleId(Integer aprobadorErleId) { this.aprobadorErleId = aprobadorErleId; }

    public LocalDateTime getFechaAprobacionErle() { return fechaAprobacionErle; }
    public void setFechaAprobacionErle(LocalDateTime fechaAprobacionErle) { this.fechaAprobacionErle = fechaAprobacionErle; }

    public Integer getAprobadorEnlId() { return aprobadorEnlId; }
    public void setAprobadorEnlId(Integer aprobadorEnlId) { this.aprobadorEnlId = aprobadorEnlId; }

    public Integer getRechazadoPorId() { return rechazadoPorId; }
    public void setRechazadoPorId(Integer rechazadoPorId) { this.rechazadoPorId = rechazadoPorId; }

    public LocalDateTime getFechaRechazo() { return fechaRechazo; }
    public void setFechaRechazo(LocalDateTime fechaRechazo) { this.fechaRechazo = fechaRechazo; }
}
