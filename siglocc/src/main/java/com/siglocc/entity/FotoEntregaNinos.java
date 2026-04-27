package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Foto de evidencia de la entrega de cajas a los niños en la iglesia (Momento C).
 *
 * <p>Mapea la tabla {@code fotos_entrega_ninos}. Estas fotos muestran el momento
 * en que los niños reciben físicamente las cajas OCC dentro de la iglesia.
 * Cualquier equipo (ERL, ERLE o ENL) puede cargarlas.</p>
 *
 * <p>A diferencia de las fotos de entrega a iglesias (Momento B), estas fotos
 * se asocian directamente a la iglesia y temporada, ya que el acto de entrega
 * a los niños ocurre en un momento posterior a la distribución logística.</p>
 */
@Entity
@Table(name = "fotos_entrega_ninos")
public class FotoEntregaNinos {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la iglesia donde se realizó la entrega a los niños. */
    @Column(name = "iglesia_id", nullable = false)
    private Integer iglesiaId;

    /** ID de la temporada a la que corresponde la entrega. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Nombre/ruta del archivo de imagen almacenado en disco. */
    @Column(nullable = false, length = 500)
    private String url;

    /** Orden de la foto dentro del conjunto de evidencias de la iglesia. */
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

    public Integer getIglesiaId() { return iglesiaId; }
    public void setIglesiaId(Integer iglesiaId) { this.iglesiaId = iglesiaId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Integer getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Integer subidoPor) { this.subidoPor = subidoPor; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
