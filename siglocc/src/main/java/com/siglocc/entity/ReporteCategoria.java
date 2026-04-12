package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Entidad maestra que define los rubros disponibles para reportar gastos mensuales.
 *
 * <p>Mapea la tabla {@code reporte_categorias}. Cada categoría tiene un código único
 * (p. ej. {@code E-1}, {@code M-2}, {@code O-1}), pertenece a una familia y tiene
 * un nombre descriptivo.</p>
 *
 * <p><strong>Nota de despliegue:</strong> Los registros iniciales (E-0 hasta O-1)
 * deben insertarse manualmente en la base de datos antes de usar el módulo de
 * reportes. Hibernate no genera datos semilla automáticamente.</p>
 *
 * <p>Ejemplo de categorías:</p>
 * <pre>
 *   codigo | familia | nombre_largo
 *   -------|---------|-----------------------------
 *   E-0    |  E      | Materiales de entrenamiento
 *   E-1    |  E      | Refrigerios punto de venta
 *   M-1    |  M      | Transporte mentoría
 *   O-1    |  O      | Gastos administrativos varios
 * </pre>
 */
@Entity
@Table(name = "reporte_categorias")
public class ReporteCategoria {

    /**
     * Código único de la categoría (p. ej. {@code E-1}, {@code M-2}).
     * Es la clave primaria natural de la tabla; no se autogenera.
     */
    @Id
    @Column(length = 10)
    private String codigo;

    /**
     * Familia a la que pertenece la categoría.
     * Controla qué tipos de equipo pueden usar este rubro al reportar.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private FamiliaCategoria familia;

    /** Descripción completa y legible de la categoría de gasto. */
    @Column(name = "nombre_largo", nullable = false, length = 255)
    private String nombreLargo;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public FamiliaCategoria getFamilia() { return familia; }
    public void setFamilia(FamiliaCategoria familia) { this.familia = familia; }

    public String getNombreLargo() { return nombreLargo; }
    public void setNombreLargo(String nombreLargo) { this.nombreLargo = nombreLargo; }
}
