package com.siglocc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad {@link VistaControlSaldos}.
 *
 * <p>La vista {@code vista_control_saldos_enl} no tiene una columna {@code id}
 * simple. En su lugar, la combinación de {@code equipo_id} y {@code temporada_id}
 * identifica de forma única una fila. JPA requiere que las claves compuestas
 * estén en una clase separada anotada con {@code @Embeddable}.</p>
 *
 * <p>Esta clase implementa {@link Serializable} y sobreescribe {@code equals}
 * y {@code hashCode}, requisitos obligatorios de JPA para claves embebidas.</p>
 */
@Embeddable
public class VistaControlSaldosId implements Serializable {

    /** ID del equipo al que pertenece el presupuesto. */
    @Column(name = "equipo_id")
    private Integer equipoId;

    /** ID de la temporada a la que pertenece el presupuesto. */
    @Column(name = "temporada_id")
    private Integer temporadaId;

    /** Constructor requerido por JPA. */
    public VistaControlSaldosId() {}

    /**
     * Constructor de conveniencia usado en el service para construir
     * la clave y consultar la vista.
     *
     * @param equipoId    ID del equipo
     * @param temporadaId ID de la temporada activa
     */
    public VistaControlSaldosId(Integer equipoId, Integer temporadaId) {
        this.equipoId = equipoId;
        this.temporadaId = temporadaId;
    }

    public Integer getEquipoId() { return equipoId; }
    public Integer getTemporadaId() { return temporadaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VistaControlSaldosId that)) return false;
        return Objects.equals(equipoId, that.equipoId) && Objects.equals(temporadaId, that.temporadaId);
    }

    @Override
    public int hashCode() { return Objects.hash(equipoId, temporadaId); }
}
