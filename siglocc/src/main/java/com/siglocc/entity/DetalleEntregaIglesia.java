package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Detalle de un ítem entregado en el acta de entrega a una iglesia.
 *
 * <p>Mapea la tabla {@code detalle_entrega_iglesia}. Cada fila representa una línea
 * del acta: cuántas unidades de qué tipo de ítem (OE, NT, EMR, LGA) se entregaron
 * a la iglesia.</p>
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

    /** ID del tipo de ítem entregado (referencia a {@code tipos_item}). */
    @Column(name = "tipo_item_id", nullable = false)
    private Integer tipoItemId;

    /** Cantidad de unidades de este ítem entregadas a la iglesia. */
    @Column(name = "cantidad_entregada", nullable = false)
    private Integer cantidadEntregada;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEntregaId() { return entregaId; }
    public void setEntregaId(Integer entregaId) { this.entregaId = entregaId; }

    public Integer getTipoItemId() { return tipoItemId; }
    public void setTipoItemId(Integer tipoItemId) { this.tipoItemId = tipoItemId; }

    public Integer getCantidadEntregada() { return cantidadEntregada; }
    public void setCantidadEntregada(Integer cantidadEntregada) { this.cantidadEntregada = cantidadEntregada; }
}
