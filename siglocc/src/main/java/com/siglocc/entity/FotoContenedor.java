package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Foto de evidencia de la llegada de un contenedor a una región (Momento A).
 *
 * <p>Mapea la tabla {@code fotos_contenedor}. Se permiten hasta 4 fotos por recepción,
 * siendo obligatoria la primera (orden = 1) que debe mostrar el número en la puerta
 * del contenedor.</p>
 */
@Entity
@Table(name = "fotos_contenedor")
public class FotoContenedor {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la recepción de contenedor a la que pertenece esta foto. */
    @Column(name = "recepcion_id", nullable = false)
    private Integer recepcionId;

    /** Nombre/ruta del archivo de imagen almacenado en disco. */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Orden de la foto dentro de la recepción (1 a 4).
     * La foto con orden = 1 es obligatoria y debe mostrar el número del contenedor.
     */
    @Column(nullable = false)
    private Integer orden;

    /** Descripción opcional del contenido de la foto. */
    @Column(length = 200)
    private String descripcion;

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

    public Integer getRecepcionId() { return recepcionId; }
    public void setRecepcionId(Integer recepcionId) { this.recepcionId = recepcionId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Integer subidoPor) { this.subidoPor = subidoPor; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
