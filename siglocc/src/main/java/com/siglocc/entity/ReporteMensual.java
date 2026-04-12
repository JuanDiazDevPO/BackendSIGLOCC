package com.siglocc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad cabezote del reporte mensual de gastos de un equipo.
 *
 * <p>Mapea la tabla {@code reportes_mensuales}. Un reporte agrupa todos los rubros
 * gastados por un equipo durante un mes específico y controla el estado del flujo
 * de aprobación (BORRADOR → PENDIENTE_ERLE → PENDIENTE_ENL → APROBADO).</p>
 *
 * <p><strong>Restricción de unicidad:</strong> La combinación
 * {@code (equipo_id, temporada_id, mes, anio)} es única en la tabla,
 * lo que garantiza que no se registren reportes duplicados para el mismo
 * período. Intentar crear un duplicado resultará en un
 * {@link IllegalArgumentException} antes de llegar a la base de datos.</p>
 *
 * <p><strong>Impacto financiero:</strong> Solo los reportes en estado
 * {@link EstadoReporte#APROBADO} son tenidos en cuenta por las vistas MySQL
 * para calcular el ejecutado real del equipo.</p>
 */
@Entity
@Table(
    name = "reportes_mensuales",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reporte_equipo_mes",
        columnNames = {"equipo_id", "temporada_id", "mes", "anio"}
    )
)
public class ReporteMensual {

    /** Identificador único autoincremental del reporte. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID del equipo que genera el reporte.
     * Se extrae del token JWT al momento de la creación; el cliente no puede
     * enviarlo en el body para evitar suplantación.
     */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada a la que pertenece este reporte. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Mes del reporte (1 = enero, 12 = diciembre). */
    @Column(nullable = false)
    private Integer mes;

    /** Año del reporte (p. ej. 2025). */
    @Column(nullable = false)
    private Integer anio;

    /**
     * Ruta o nombre del archivo de soporte (PDF/ZIP) que evidencia los gastos.
     * Es {@code null} mientras el reporte está en estado {@link EstadoReporte#BORRADOR}.
     * El archivo se renombra con la convención {@code SOPORTE_EQ{id}_MES{m}_{a}.ext}.
     */
    @Column(name = "url_soporte", length = 500)
    private String urlSoporte;

    /**
     * Estado actual en el flujo de aprobación.
     * Ver {@link EstadoReporte} para el diagrama completo de transiciones.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReporte estado;

    /**
     * Motivo de rechazo o comentarios del revisor.
     * Obligatorio cuando el estado cambia a {@link EstadoReporte#RECHAZADO}.
     */
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /** Marca de tiempo de creación del reporte. Se asigna automáticamente en el servicio. */
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora en que el ENL aprobó definitivamente el reporte.
     * Es {@code null} hasta que el estado cambia a {@link EstadoReporte#APROBADO}.
     */
    @Column(name = "fecha_aprobacion_final")
    private LocalDateTime fechaAprobacionFinal;

    /**
     * ID del usuario ERLE que dio el primer visto bueno al reporte.
     * Se registra cuando el estado transiciona de {@link EstadoReporte#PENDIENTE_ERLE}
     * a {@link EstadoReporte#PENDIENTE_ENL}. Es {@code null} hasta ese momento.
     */
    @Column(name = "aprobador_erle_id")
    private Integer aprobadorErleId;

    /**
     * ID del usuario ENL que cerró el proceso de aprobación.
     * Se registra cuando el estado transiciona de {@link EstadoReporte#PENDIENTE_ENL}
     * a {@link EstadoReporte#APROBADO} o {@link EstadoReporte#RECHAZADO}.
     * Es {@code null} hasta ese momento.
     */
    @Column(name = "aprobador_enl_id")
    private Integer aprobadorEnlId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getUrlSoporte() { return urlSoporte; }
    public void setUrlSoporte(String urlSoporte) { this.urlSoporte = urlSoporte; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaAprobacionFinal() { return fechaAprobacionFinal; }
    public void setFechaAprobacionFinal(LocalDateTime fechaAprobacionFinal) { this.fechaAprobacionFinal = fechaAprobacionFinal; }

    public Integer getAprobadorErleId() { return aprobadorErleId; }
    public void setAprobadorErleId(Integer aprobadorErleId) { this.aprobadorErleId = aprobadorErleId; }

    public Integer getAprobadorEnlId() { return aprobadorEnlId; }
    public void setAprobadorEnlId(Integer aprobadorEnlId) { this.aprobadorEnlId = aprobadorEnlId; }
}
