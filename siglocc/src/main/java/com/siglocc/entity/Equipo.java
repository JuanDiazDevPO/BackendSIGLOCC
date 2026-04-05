package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa un equipo de trabajo dentro del sistema SIGLOCC.
 *
 * <p>Mapea la tabla {@code equipos} de la base de datos. Los equipos forman
 * una jerarquía auto-referencial de tres niveles: ENL → ERLE → ERL.</p>
 *
 * <p>Para evitar problemas de {@code LazyInitializationException} al construir
 * las respuestas de la API, las referencias al equipo padre ({@code enl_id} y
 * {@code erle_id}) se exponen directamente como columnas de solo lectura
 * ({@code insertable=false, updatable=false}) en lugar de relaciones
 * {@code @ManyToOne}.</p>
 */
@Entity
@Table(name = "equipos")
public class Equipo {

    /** Identificador único autoincremental del equipo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre descriptivo del equipo (ej: "Equipo Nacional de Liderazgo"). */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Tipo de equipo según la jerarquía organizacional.
     * Se almacena como texto en la BD usando el nombre del enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEquipo tipo;

    /**
     * ID del equipo ENL al que pertenece este equipo.
     * Es {@code null} si el equipo es de tipo {@code ENL} (nivel máximo).
     * Columna de solo lectura; no se gestiona desde JPA.
     */
    @Column(name = "enl_id", insertable = false, updatable = false)
    private Integer enlId;

    /**
     * ID del equipo ERLE al que pertenece este equipo.
     * Solo aplica para equipos de tipo {@code ERL}.
     * Columna de solo lectura; no se gestiona desde JPA.
     */
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
