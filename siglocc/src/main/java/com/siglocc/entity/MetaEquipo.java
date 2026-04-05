package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa la meta de contenedores asignada a un equipo por temporada.
 *
 * <p>Mapea la tabla {@code metas_equipo}. Es el <strong>prerequisito obligatorio</strong>
 * antes de que un equipo pueda ingresar sus datos de presupuesto: sin meta asignada,
 * el sistema rechaza la creación de {@link PresupuestoDatos}.</p>
 *
 * <p>La meta de contenedores es el punto de partida del cálculo financiero.
 * La vista de presupuesto usa este valor junto con el promedio CM para derivar
 * los indicadores LGA y los presupuestos por rubro.</p>
 */
@Entity
@Table(name = "metas_equipo")
public class MetaEquipo {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID del equipo al que se le asigna esta meta. */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada para la que aplica la meta. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Número de contenedores meta asignados al equipo para la temporada. */
    @Column(name = "meta_contenedores", nullable = false)
    private Integer metaContenedores;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public Integer getMetaContenedores() { return metaContenedores; }
    public void setMetaContenedores(Integer metaContenedores) { this.metaContenedores = metaContenedores; }
}
