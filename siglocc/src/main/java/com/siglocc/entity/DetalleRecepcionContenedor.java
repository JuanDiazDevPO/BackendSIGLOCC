package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Desglose de un ítem recibido en un contenedor.
 *
 * <p>Mapea la tabla {@code detalle_recepcion_contenedor}. Cada fila representa
 * una categoría de caja o un tipo de literatura que llegó en el contenedor.</p>
 *
 * <p><strong>Regla de exclusividad:</strong> exactamente uno de
 * {@code categoriaCajaId} o {@code tipoItemId} debe ser no nulo por fila.
 * La restricción {@code chk_det_rec_exactamente_uno} en BD lo garantiza.</p>
 */
@Entity
@Table(name = "detalle_recepcion_contenedor")
public class DetalleRecepcionContenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la recepción de contenedor a la que pertenece este detalle. */
    @Column(name = "recepcion_id", nullable = false)
    private Integer recepcionId;

    /**
     * Categoría de caja OCC (NINO_2_4, NINA_5_9…).
     * Es nulo cuando la fila representa literatura.
     */
    @Column(name = "categoria_caja_id")
    private Integer categoriaCajaId;

    /**
     * Tipo de literatura (FOLLETO, GM, EMR, LGA, NT…).
     * Es nulo cuando la fila representa cajas.
     */
    @Column(name = "tipo_item_id")
    private Integer tipoItemId;

    /** Cantidad recibida de esta categoría/ítem en el contenedor. */
    @Column(nullable = false)
    private Integer cantidad;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getRecepcionId() { return recepcionId; }
    public void setRecepcionId(Integer recepcionId) { this.recepcionId = recepcionId; }

    public Integer getCategoriaCajaId() { return categoriaCajaId; }
    public void setCategoriaCajaId(Integer categoriaCajaId) { this.categoriaCajaId = categoriaCajaId; }

    public Integer getTipoItemId() { return tipoItemId; }
    public void setTipoItemId(Integer tipoItemId) { this.tipoItemId = tipoItemId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
