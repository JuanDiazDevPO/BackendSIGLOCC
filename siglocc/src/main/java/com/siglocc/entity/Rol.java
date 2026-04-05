package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa un rol del sistema.
 *
 * <p>Mapea la tabla {@code roles} de la base de datos. Cada usuario tiene
 * asignado exactamente un rol, que determina qué acciones puede realizar
 * dentro del sistema (ver {@link Usuario}).</p>
 *
 * <p>Ejemplos de roles existentes:</p>
 * <ul>
 *   <li>{@code ENL_RECURSOS} – puede crear usuarios y aprobar anticipos</li>
 *   <li>{@code ENL_LOGISTICA} – puede crear usuarios</li>
 *   <li>{@code ERLE} – puede crear solicitudes de anticipo</li>
 * </ul>
 *
 * <p>El campo {@code nombre} se almacena como {@code nombre} en la BD pero
 * se expone en Java como {@code name} por convención del proyecto.</p>
 */
@Entity
@Table(name = "roles")
public class Rol {

    /** Identificador único autoincremental del rol. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Nombre del rol (columna {@code nombre} en BD).
     * Se usa directamente como autoridad en Spring Security,
     * prefijado con {@code ROLE_} por {@link com.siglocc.service.UserDetailsServiceImpl}.
     */
    @Column(name = "nombre")
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
