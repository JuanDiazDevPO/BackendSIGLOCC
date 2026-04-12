package com.siglocc.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;

/**
 * Entidad que representa un usuario registrado en el sistema SIGLOCC.
 *
 * <p>Mapea la tabla {@code usuarios} de la base de datos. Cada usuario pertenece
 * a un equipo y tiene asignado un rol que controla sus permisos en la aplicación.</p>
 *
 * <p><strong>Notas importantes:</strong></p>
 * <ul>
 *   <li>El campo {@code password} siempre debe almacenarse hasheado con BCrypt.
 *       Nunca se guarda en texto plano.</li>
 *   <li>El login se realiza con {@code email}, no con un nombre de usuario.</li>
 *   <li>El campo {@code activo} permite deshabilitar un usuario sin eliminarlo
 *       de la BD. Un usuario inactivo no puede iniciar sesión.</li>
 * </ul>
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Identificador único autoincremental del usuario. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre de pila del usuario. */
    @Column(nullable = false, length = 50)
    private String name;

    /** Apellido del usuario. */
    @Column(nullable = false, length = 50)
    private String lastname;

    /**
     * Contraseña del usuario hasheada con BCrypt.
     * Nunca se retorna en las respuestas de la API.
     */
    @Column(nullable = false)
    private String password;

    /** Correo electrónico único. Se usa como identificador de login. */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * Rol asignado al usuario. Cargado de forma {@code EAGER} para que
     * Spring Security pueda construir las autoridades en el mismo query
     * que carga el usuario.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Rol rol;

    /**
     * Equipo al que pertenece el usuario (ej: Antioquia, ENL).
     * Cargado de forma {@code EAGER} porque se necesita en el login
     * para construir la respuesta con los detalles del equipo.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    /**
     * Indica si el usuario está habilitado para iniciar sesión.
     * Un valor {@code false} bloquea el acceso sin eliminar el registro.
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean activo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Entidad JPA gestionada por Hibernate; la copia defensiva rompería el seguimiento de cambios del contexto de persistencia.")
    public Rol getRol() { return rol; }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Entidad JPA gestionada por Hibernate; la copia defensiva rompería la identidad de entidad requerida por el contexto de persistencia.")
    public void setRol(Rol rol) { this.rol = rol; }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Entidad JPA gestionada por Hibernate; la copia defensiva rompería el seguimiento de cambios del contexto de persistencia.")
    public Equipo getEquipo() { return equipo; }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Entidad JPA gestionada por Hibernate; la copia defensiva rompería la identidad de entidad requerida por el contexto de persistencia.")
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
