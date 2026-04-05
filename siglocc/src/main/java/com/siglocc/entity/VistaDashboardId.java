package com.siglocc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la vista {@code vista_dashboard_financiero}.
 *
 * <p>La vista tiene datos de múltiples equipos y temporadas, por lo que la
 * combinación {@code equipo_id + temporada_id} identifica unívocamente cada fila.</p>
 *
 * <p>Debe implementar {@link Serializable}, {@link #equals} y {@link #hashCode}
 * como lo exige JPA para claves compuestas con {@code @EmbeddedId}.</p>
 */
@Embeddable
public class VistaDashboardId implements Serializable {

    /** ID del equipo dentro de la vista. */
    @Column(name = "equipo_id")
    private Integer equipoId;

    /** ID de la temporada dentro de la vista. */
    @Column(name = "temporada_id")
    private Integer temporadaId;

    public VistaDashboardId() {}

    public VistaDashboardId(Integer equipoId, Integer temporadaId) {
        this.equipoId = equipoId;
        this.temporadaId = temporadaId;
    }

    public Integer getEquipoId() { return equipoId; }
    public Integer getTemporadaId() { return temporadaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VistaDashboardId)) return false;
        VistaDashboardId that = (VistaDashboardId) o;
        return Objects.equals(equipoId, that.equipoId) &&
               Objects.equals(temporadaId, that.temporadaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipoId, temporadaId);
    }
}
