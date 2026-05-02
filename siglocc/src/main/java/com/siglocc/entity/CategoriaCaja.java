package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Categoría de caja OCC según los rangos estándar de Samaritan's Purse.
 *
 * <p>Mapea la tabla {@code categorias_caja}. Existen 6 categorías:
 * género (NINO/NINA) × rango de edad (2-4, 5-9, 10-14).</p>
 *
 * <p>Son las unidades del inventario de cajas. Todo el stock,
 * asignaciones y entregas de cajas OCC se desglosan por categoría.</p>
 */
@Entity
@Table(name = "categorias_caja")
public class CategoriaCaja {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código abreviado de la categoría.
     * Ejemplos: {@code NINO_2_4}, {@code NINA_5_9}, {@code NINO_10_14}.
     */
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    /** Género al que va destinada la caja. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneroCategoria genero;

    /** Edad mínima del rango (inclusive). */
    @Column(name = "edad_min", nullable = false)
    private Integer edadMin;

    /** Edad máxima del rango (inclusive). */
    @Column(name = "edad_max", nullable = false)
    private Integer edadMax;

    /** Descripción legible. Ej: «Niño 5-9 años». */
    @Column(nullable = false, length = 100)
    private String descripcion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public GeneroCategoria getGenero() { return genero; }
    public void setGenero(GeneroCategoria genero) { this.genero = genero; }

    public Integer getEdadMin() { return edadMin; }
    public void setEdadMin(Integer edadMin) { this.edadMin = edadMin; }

    public Integer getEdadMax() { return edadMax; }
    public void setEdadMax(Integer edadMax) { this.edadMax = edadMax; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
