package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Detalle de un ítem entregado en el acta de entrega a una iglesia.
 *
 * <p>Mapea la tabla {@code detalle_entrega_iglesia}. Cada fila representa una línea
 * del acta: cuántas unidades de qué categoría de caja o qué tipo de literatura
 * (FOLLETO, GM, MPG, EMR, LGA, NT) se entregaron a la iglesia.</p>
 *
 * <p><strong>Regla de exclusividad:</strong> exactamente uno de {@code categoriaCajaId}
 * o {@code tipoItemId} debe ser no nulo por fila — igual que en
 * {@code DetalleRecepcionContenedor} y {@code AsignacionDetalle}. La restricción
 * {@code chk_det_ent_exactamente_uno} en BD lo garantiza; el campo {@code categoriaCajaId}
 * ya existía en la columna real desde que se creó la tabla, pero no estaba mapeado
 * aquí ni expuesto por la API hasta ahora.</p>
 */
@Entity
@Table(name = "detalle_entrega_iglesia")
public class DetalleEntregaIglesia {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID del acta de entrega a la que pertenece este detalle. */
    @Column(name = "entrega_id", nullable = false)
    private Integer entregaId;

    /**
     * Categoría de caja OCC entregada (NINO_2_4, NINA_5_9…).
     * Es nulo cuando la fila representa literatura.
     */
    @Column(name = "categoria_caja_id")
    private Integer categoriaCajaId;

    /**
     * ID del tipo de ítem entregado (referencia a {@code tipos_item}).
     * Es nulo cuando la fila representa cajas.
     */
    @Column(name = "tipo_item_id")
    private Integer tipoItemId;

    /** Cantidad de unidades de este ítem/categoría entregadas a la iglesia. */
    @Column(name = "cantidad_entregada", nullable = false)
    private Integer cantidadEntregada;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEntregaId() { return entregaId; }
    public void setEntregaId(Integer entregaId) { this.entregaId = entregaId; }

    public Integer getCategoriaCajaId() { return categoriaCajaId; }
    public void setCategoriaCajaId(Integer categoriaCajaId) { this.categoriaCajaId = categoriaCajaId; }

    public Integer getTipoItemId() { return tipoItemId; }
    public void setTipoItemId(Integer tipoItemId) { this.tipoItemId = tipoItemId; }

    public Integer getCantidadEntregada() { return cantidadEntregada; }
    public void setCantidadEntregada(Integer cantidadEntregada) { this.cantidadEntregada = cantidadEntregada; }
}
