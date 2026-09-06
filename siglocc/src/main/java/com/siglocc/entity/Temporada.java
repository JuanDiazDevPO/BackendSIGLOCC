package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidad que representa una temporada o ciclo operativo del sistema SIGLOCC.
 *
 * <p>Mapea la tabla {@code temporadas}. Una temporada define el período
 * presupuestal activo (ej: "Temporada 2025-2026"). Los presupuestos, los
 * saldos y las solicitudes de anticipo están siempre asociados a una
 * temporada específica.</p>
 *
 * <p>Solo puede existir <strong>una temporada activa</strong> a la vez,
 * identificada por el campo {@code es_actual = true}. El sistema la
 * detecta automáticamente al crear solicitudes de anticipo, sin necesidad
 * de que el usuario la especifique.</p>
 */
@Entity
@Table(name = "temporadas")
public class Temporada {

    /** Identificador único autoincremental de la temporada. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre descriptivo de la temporada (ej: "Temporada 2025-2026"). */
    private String nombre;

    /** Fecha en que inicia la temporada. */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    /** Fecha en que finaliza la temporada. */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /**
     * Indica si esta es la temporada actualmente activa.
     * El sistema usa este campo para asociar automáticamente las solicitudes
     * de anticipo a la temporada correcta.
     */
    @Column(name = "es_actual")
    private boolean esActual;

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isEsActual() { return esActual; }
    public void setEsActual(boolean esActual) { this.esActual = esActual; }
}
