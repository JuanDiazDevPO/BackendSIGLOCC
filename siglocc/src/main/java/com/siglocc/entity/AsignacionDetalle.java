package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Detalle de una asignación: cuántas unidades de una categoría de caja
 * o tipo de literatura le corresponden a una iglesia específica.
 *
 * <p>Mapea la tabla {@code asignacion_detalle}. El campo
 * {@code ajustadaManualmente} queda en {@code true} si el coordinador
 * modifica la cantidad calculada automáticamente por el Motor Hamilton.</p>
 *
 * <p><strong>Regla de exclusividad:</strong> exactamente uno de
 * {@code categoriaCajaId} o {@code tipoItemId} debe ser no nulo.</p>
 */
@Entity
@Table(name = "asignacion_detalle")
public class AsignacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la cabecera de asignación a la que pertenece este detalle. */
    @Column(name = "cabecera_id", nullable = false)
    private Integer cabeceraId;

    /** ID de la iglesia que recibe esta asignación. */
    @Column(name = "iglesia_id", nullable = false)
    private Integer iglesiaId;

    /**
     * Categoría de caja asignada.
     * Nulo si la fila corresponde a literatura.
     */
    @Column(name = "categoria_caja_id")
    private Integer categoriaCajaId;

    /**
     * Tipo de literatura asignada.
     * Nulo si la fila corresponde a cajas.
     */
    @Column(name = "tipo_item_id")
    private Integer tipoItemId;

    /** Cantidad asignada a esta iglesia para esta categoría/ítem. */
    @Column(name = "cantidad_asignada", nullable = false)
    private Integer cantidadAsignada;

    /**
     * Indica si el coordinador modificó manualmente esta línea
     * después de la generación automática.
     */
    @Column(name = "ajustada_manualmente", nullable = false)
    private Boolean ajustadaManualmente = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCabeceraId() { return cabeceraId; }
    public void setCabeceraId(Integer cabeceraId) { this.cabeceraId = cabeceraId; }

    public Integer getIglesiaId() { return iglesiaId; }
    public void setIglesiaId(Integer iglesiaId) { this.iglesiaId = iglesiaId; }

    public Integer getCategoriaCajaId() { return categoriaCajaId; }
    public void setCategoriaCajaId(Integer categoriaCajaId) { this.categoriaCajaId = categoriaCajaId; }

    public Integer getTipoItemId() { return tipoItemId; }
    public void setTipoItemId(Integer tipoItemId) { this.tipoItemId = tipoItemId; }

    public Integer getCantidadAsignada() { return cantidadAsignada; }
    public void setCantidadAsignada(Integer cantidadAsignada) { this.cantidadAsignada = cantidadAsignada; }

    public Boolean getAjustadaManualmente() { return ajustadaManualmente; }
    public void setAjustadaManualmente(Boolean ajustadaManualmente) { this.ajustadaManualmente = ajustadaManualmente; }
}
