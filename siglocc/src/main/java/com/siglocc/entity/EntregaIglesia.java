package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Acta de entrega de cajas y literatura a una iglesia (Momento 3 – La Entrega).
 *
 * <p>Mapea la tabla {@code entregas_iglesia}. Registra la entrega física de los
 * materiales (cajas OCC, NT, EMR, LGA) a una iglesia aprobada. Los detalles
 * por tipo de ítem se almacenan en {@code DetalleEntregaIglesia}.</p>
 *
 * <p><strong>Restricción:</strong> una iglesia solo puede tener un acta de entrega
 * por temporada ({@code UNIQUE(iglesia_id, temporada_id)}).</p>
 */
@Entity
@Table(
    name = "entregas_iglesia",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_entrega_iglesia_temporada",
        columnNames = {"iglesia_id", "temporada_id"}
    )
)
public class EntregaIglesia {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la iglesia que recibe el material. */
    @Column(name = "iglesia_id", nullable = false)
    private Integer iglesiaId;

    /** ID del punto de entrega donde se realiza la distribución. */
    @Column(name = "punto_entrega_id", nullable = false)
    private Integer puntoEntregaId;

    /** ID de la temporada a la que corresponde esta entrega. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /**
     * ID del equipo que coordina la entrega.
     * Se extrae del JWT al momento de crear el acta.
     */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** Fecha en que se realizó la entrega física. */
    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    /**
     * Tipo de firma del acta de entrega.
     * {@link FirmaTipo#DIGITAL} o {@link FirmaTipo#ESCANEADA}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "firma_tipo")
    private FirmaTipo firmaTipo;

    /**
     * Nombre/ruta del archivo de firma o acta escaneada.
     * Es {@code null} hasta que se sube la firma con el endpoint correspondiente.
     */
    @Column(name = "firma_url", length = 500)
    private String firmaUrl;

    /**
     * Estado del acta de entrega.
     * Inicia en {@link EstadoEntrega#PENDIENTE} al crear el registro.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    /** Observaciones sobre la entrega (novedades, parcialidades, etc.). */
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /**
     * Indica si el coordinador confirmó la recepción por parte de la iglesia.
     * Inicia en {@code false}; cambia a {@code true} al completar la entrega.
     */
    @Column(nullable = false)
    private Boolean confirmado = false;

    /** Fecha y hora en que se creó el acta en el sistema. */
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIglesiaId() { return iglesiaId; }
    public void setIglesiaId(Integer iglesiaId) { this.iglesiaId = iglesiaId; }

    public Integer getPuntoEntregaId() { return puntoEntregaId; }
    public void setPuntoEntregaId(Integer puntoEntregaId) { this.puntoEntregaId = puntoEntregaId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public FirmaTipo getFirmaTipo() { return firmaTipo; }
    public void setFirmaTipo(FirmaTipo firmaTipo) { this.firmaTipo = firmaTipo; }

    public String getFirmaUrl() { return firmaUrl; }
    public void setFirmaUrl(String firmaUrl) { this.firmaUrl = firmaUrl; }

    public EstadoEntrega getEstado() { return estado; }
    public void setEstado(EstadoEntrega estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Boolean getConfirmado() { return confirmado; }
    public void setConfirmado(Boolean confirmado) { this.confirmado = confirmado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
