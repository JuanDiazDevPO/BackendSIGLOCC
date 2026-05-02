package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Iglesia inscrita en la operación logística de una temporada (Momento 1 – La Visión).
 *
 * <p>Mapea la tabla {@code iglesias}. Cada iglesia se asocia a un equipo ERL y a una
 * temporada específica; la misma iglesia puede participar en varias temporadas como
 * registros independientes.</p>
 *
 * <p><strong>Restricción de unicidad:</strong> la combinación
 * {@code (nombre, equipo_id, temporada_id)} es única para evitar registros duplicados
 * dentro del mismo equipo y temporada.</p>
 *
 * <p><strong>Flujo de estados:</strong>
 * {@code PENDIENTE → APROBADA | RECHAZADA}</p>
 *
 * <p>Solo las iglesias {@link EstadoIglesia#APROBADA} participan en el Momento 2
 * (Capacitación) y el Momento 3 (Entrega de cajas y literatura).</p>
 */
@Entity
@Table(
    name = "iglesias",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_iglesia_nombre_equipo_temporada",
        columnNames = {"nombre", "equipo_id", "temporada_id"}
    )
)
public class Iglesia {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre de la iglesia. */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Denominación o movimiento religioso al que pertenece (opcional). */
    @Column(length = 100)
    private String denominacion;

    /** Departamento donde se ubica la iglesia. */
    @Column(nullable = false, length = 50)
    private String departamento;

    /** Ciudad donde se ubica la iglesia. */
    @Column(nullable = false, length = 50)
    private String ciudad;

    /** Dirección detallada de la iglesia. */
    @Column(columnDefinition = "TEXT")
    private String direccion;

    // ── Datos del pastor ────────────────────────────────────────────────────

    /** Nombre completo del pastor de la iglesia. */
    @Column(name = "pastor_nombre", length = 100)
    private String pastorNombre;

    /** Celular de contacto del pastor. */
    @Column(name = "pastor_celular", length = 20)
    private String pastorCelular;

    /** Correo electrónico del pastor. */
    @Column(name = "pastor_correo", length = 100)
    private String pastorCorreo;

    // ── Datos del líder de contacto ─────────────────────────────────────────

    /** Nombre completo del líder de contacto en la iglesia. */
    @Column(name = "nombre_lider", length = 100)
    private String nombreLider;

    /** Celular de contacto del líder. */
    @Column(name = "celular_lider", length = 20)
    private String celularLider;

    /** Correo electrónico del líder de contacto. */
    @Column(name = "correo_lider", length = 100)
    private String correoLider;

    // ── Contexto y solicitud ────────────────────────────────────────────────

    /**
     * ID del equipo ERL responsable de esta iglesia.
     * Se extrae del JWT al momento del registro.
     */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada a la que pertenece este registro. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /**
     * Número de cajas OCC solicitadas por la iglesia.
     * Se asigna formalmente al aprobar la iglesia según la capacitación.
     */
    @Column(name = "cajas_solicitadas")
    private Integer cajasSolicitadas;

    /**
     * Estado de la solicitud de participación.
     * Inicia en {@link EstadoIglesia#PENDIENTE} al registrarse.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIglesia estado = EstadoIglesia.PENDIENTE;

    /**
     * Justificación del rechazo, obligatoria cuando el estado cambia a
     * {@link EstadoIglesia#RECHAZADA}.
     */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    /** Fecha y hora en que se registró la iglesia en el sistema. */
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDenominacion() { return denominacion; }
    public void setDenominacion(String denominacion) { this.denominacion = denominacion; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getPastorNombre() { return pastorNombre; }
    public void setPastorNombre(String pastorNombre) { this.pastorNombre = pastorNombre; }

    public String getPastorCelular() { return pastorCelular; }
    public void setPastorCelular(String pastorCelular) { this.pastorCelular = pastorCelular; }

    public String getPastorCorreo() { return pastorCorreo; }
    public void setPastorCorreo(String pastorCorreo) { this.pastorCorreo = pastorCorreo; }

    public String getNombreLider() { return nombreLider; }
    public void setNombreLider(String nombreLider) { this.nombreLider = nombreLider; }

    public String getCelularLider() { return celularLider; }
    public void setCelularLider(String celularLider) { this.celularLider = celularLider; }

    public String getCorreoLider() { return correoLider; }
    public void setCorreoLider(String correoLider) { this.correoLider = correoLider; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public Integer getCajasSolicitadas() { return cajasSolicitadas; }
    public void setCajasSolicitadas(Integer cajasSolicitadas) { this.cajasSolicitadas = cajasSolicitadas; }

    public EstadoIglesia getEstado() { return estado; }
    public void setEstado(EstadoIglesia estado) { this.estado = estado; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
