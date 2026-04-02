package com.siglocc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEquipo tipo;

    @Column(name = "enl_id", insertable = false, updatable = false)
    private Integer enlId;

    @Column(name = "erle_id", insertable = false, updatable = false)
    private Integer erleId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoEquipo getTipo() { return tipo; }
    public void setTipo(TipoEquipo tipo) { this.tipo = tipo; }

    public Integer getEnlId() { return enlId; }
    public void setEnlId(Integer enlId) { this.enlId = enlId; }

    public Integer getErleId() { return erleId; }
    public void setErleId(Integer erleId) { this.erleId = erleId; }
}
