package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de llegada de un contenedor a un punto de entrega (Momento A de fotos).
 *
 * <p>Mapea la tabla {@code recepcion_contenedores}. Cada contenedor que llega a una
 * región genera un registro con:</p>
 * <ul>
 *   <li>Información del contenedor (número, fecha, cajas recibidas).</li>
 *   <li>Referencias a documentos adjuntos (lista de transportadora, documento ABC).</li>
 *   <li>Hasta 4 fotos de evidencia, siendo obligatoria la foto del número en la puerta.</li>
 * </ul>
 */
@Entity
@Table(name = "recepcion_contenedores")
public class RecepcionContenedor {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Número identificador del contenedor (impreso en la puerta). */
    @Column(name = "numero_contenedor", nullable = false, length = 50)
    private String numeroContenedor;

    /** ID del punto de entrega donde llega el contenedor. */
    @Column(name = "punto_entrega_id", nullable = false)
    private Integer puntoEntregaId;

    /** ID de la temporada a la que pertenece esta recepción. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Fecha de llegada del contenedor. */
    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    /**
     * Total de cajas recibidas en este contenedor.
     * Es la suma de todas las categorías (NINO_2_4, NINA_5_9, etc.)
     * registradas en {@code detalle_recepcion_contenedor}.
     * Se calcula automáticamente al guardar los detalles.
     */
    @Column(name = "total_cajas_recibidas", nullable = false)
    private Integer totalCajasRecibidas;

    /**
     * ID del equipo que recibe el contenedor.
     * Se extrae del JWT al momento del registro.
     */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** Observaciones generales sobre el estado del contenedor. */
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /**
     * Nombre/ruta del archivo de lista de entrega de la transportadora.
     * Se sube como documento separado después de crear el registro.
     */
    @Column(name = "lista_transportadora_url", length = 500)
    private String listaTransportadoraUrl;

    /**
     * Nombre/ruta del documento en formato ABC interno.
     * Se sube como documento separado después de crear el registro.
     */
    @Column(name = "documento_abc_url", length = 500)
    private String documentoAbcUrl;

    /** Fecha y hora en que se registró la recepción en el sistema. */
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNumeroContenedor() { return numeroContenedor; }
    public void setNumeroContenedor(String numeroContenedor) { this.numeroContenedor = numeroContenedor; }

    public Integer getPuntoEntregaId() { return puntoEntregaId; }
    public void setPuntoEntregaId(Integer puntoEntregaId) { this.puntoEntregaId = puntoEntregaId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public LocalDate getFechaLlegada() { return fechaLlegada; }
    public void setFechaLlegada(LocalDate fechaLlegada) { this.fechaLlegada = fechaLlegada; }

    public Integer getTotalCajasRecibidas() { return totalCajasRecibidas; }
    public void setTotalCajasRecibidas(Integer totalCajasRecibidas) { this.totalCajasRecibidas = totalCajasRecibidas; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getListaTransportadoraUrl() { return listaTransportadoraUrl; }
    public void setListaTransportadoraUrl(String listaTransportadoraUrl) { this.listaTransportadoraUrl = listaTransportadoraUrl; }

    public String getDocumentoAbcUrl() { return documentoAbcUrl; }
    public void setDocumentoAbcUrl(String documentoAbcUrl) { this.documentoAbcUrl = documentoAbcUrl; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
