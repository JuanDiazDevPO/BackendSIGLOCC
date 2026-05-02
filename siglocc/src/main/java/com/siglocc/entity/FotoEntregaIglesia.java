package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Foto de evidencia de la entrega de cajas y literatura a una iglesia (Momento B).
 *
 * <p>Mapea la tabla {@code fotos_entrega_iglesia}. Estas fotos las sube cualquier
 * equipo (ERL, ERLE o ENL) y muestran el momento de la entrega física en el
 * punto de distribución.</p>
 */
@Entity
@Table(name = "fotos_entrega_iglesia")
public class FotoEntregaIglesia {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID del acta de entrega a la que pertenece esta foto. */
    @Column(name = "entrega_id", nullable = false)
    private Integer entregaId;

    /** Nombre/ruta del archivo de imagen almacenado en disco. */
    @Column(nullable = false, length = 500)
    private String url;

    /** Orden de la foto dentro del conjunto de evidencias de la entrega. */
    @Column(nullable = false)
    private Integer orden;

    /**
     * ID del usuario que subió la foto.
     * Se resuelve a partir del email del JWT.
     */
    @Column(name = "subido_por", nullable = false)
    private Integer subidoPor;

    /** Fecha y hora de carga de la foto. */
    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEntregaId() { return entregaId; }
    public void setEntregaId(Integer entregaId) { this.entregaId = entregaId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Integer getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Integer subidoPor) { this.subidoPor = subidoPor; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
