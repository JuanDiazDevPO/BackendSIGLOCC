package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Registro de la capacitación de maestros de una iglesia aprobada (Momento 2 – La Capacitación).
 *
 * <p>Mapea la tabla {@code capacitaciones_iglesia}. En el Momento 2, los maestros enviados
 * por cada iglesia asisten a una jornada de capacitación donde reciben:</p>
 * <ul>
 *   <li><strong>GM</strong> – Guía Ministerial (uno por maestro).</li>
 *   <li><strong>MPG</strong> – Libro presentación del evangelio (uno por maestro).</li>
 * </ul>
 *
 * <p><strong>Regla del número de cajas:</strong>
 * {@code cajas_calculadas = maestros_enviados × 25}.
 * Un maestro capacitado es responsable de distribuir las cajas a 25 niños.</p>
 *
 * <p><strong>Restricción:</strong> solo puede existir un registro de capacitación
 * por iglesia por temporada (restricción {@code UNIQUE(iglesia_id, temporada_id)}).</p>
 */
@Entity
@Table(
    name = "capacitaciones_iglesia",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_capacitacion_iglesia_temporada",
        columnNames = {"iglesia_id", "temporada_id"}
    )
)
public class CapacitacionIglesia {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID de la iglesia que envía maestros a la capacitación. */
    @Column(name = "iglesia_id", nullable = false)
    private Integer iglesiaId;

    /** ID de la temporada a la que corresponde esta capacitación. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Fecha en que se realizó la jornada de capacitación. */
    @Column(name = "fecha_capacitacion")
    private LocalDate fechaCapacitacion;

    /**
     * Número de maestros que asistieron efectivamente a la capacitación.
     * Determina el número de cajas asignadas: {@code maestrosEnviados × 25}.
     */
    @Column(name = "maestros_enviados", nullable = false)
    private Integer maestrosEnviados;

    /**
     * Número de cajas calculado según la regla operacional.
     * Se almacena directamente para evitar recálculos: {@code maestrosEnviados × 25}.
     */
    @Column(name = "cajas_calculadas", nullable = false)
    private Integer cajasCalculadas;

    /**
     * Cantidad de Guías Ministeriales (GM) entregadas.
     * Normalmente igual a {@code maestrosEnviados}, pero puede diferir.
     */
    @Column(name = "gm_entregados", nullable = false)
    private Integer gmEntregados;

    /**
     * Cantidad de libros de presentación del evangelio (MPG) entregados.
     * Normalmente igual a {@code maestrosEnviados}, pero puede diferir.
     */
    @Column(name = "mpg_entregados", nullable = false)
    private Integer mpgEntregados;

    /** Observaciones adicionales sobre la jornada de capacitación. */
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIglesiaId() { return iglesiaId; }
    public void setIglesiaId(Integer iglesiaId) { this.iglesiaId = iglesiaId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public LocalDate getFechaCapacitacion() { return fechaCapacitacion; }
    public void setFechaCapacitacion(LocalDate fechaCapacitacion) { this.fechaCapacitacion = fechaCapacitacion; }

    public Integer getMaestrosEnviados() { return maestrosEnviados; }
    public void setMaestrosEnviados(Integer maestrosEnviados) { this.maestrosEnviados = maestrosEnviados; }

    public Integer getCajasCalculadas() { return cajasCalculadas; }
    public void setCajasCalculadas(Integer cajasCalculadas) { this.cajasCalculadas = cajasCalculadas; }

    public Integer getGmEntregados() { return gmEntregados; }
    public void setGmEntregados(Integer gmEntregados) { this.gmEntregados = gmEntregados; }

    public Integer getMpgEntregados() { return mpgEntregados; }
    public void setMpgEntregados(Integer mpgEntregados) { this.mpgEntregados = mpgEntregados; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
