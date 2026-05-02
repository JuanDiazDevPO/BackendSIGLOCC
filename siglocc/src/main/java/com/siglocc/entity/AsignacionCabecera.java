package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cabecera de una corrida de asignación inteligente de cajas y literatura.
 *
 * <p>Mapea la tabla {@code asignacion_cabecera}. Cada vez que un coordinador
 * activa la asignación automática (o crea una manual) se genera un registro aquí,
 * junto con sus detalles en {@code AsignacionDetalle}.</p>
 *
 * <p><strong>Flujo de estados:</strong>
 * {@code BORRADOR → CONFIRMADA}</p>
 *
 * <p>Solo las asignaciones {@link EstadoAsignacion#CONFIRMADA} descuentan el
 * inventario en la vista {@code vista_inventario_disponible}. Mientras está en
 * {@link EstadoAsignacion#BORRADOR} el coordinador puede ajustar cantidades
 * manualmente antes de confirmar.</p>
 */
@Entity
@Table(name = "asignacion_cabecera")
public class AsignacionCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Equipo que generó la asignación (extraído del JWT). */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** Temporada a la que corresponde la asignación. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    /** Fecha y hora en que se generó la corrida. */
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    /**
     * {@code true} si fue generada por el motor automático (Método Hamilton).
     * {@code false} si el coordinador la construyó manualmente desde cero.
     */
    @Column(name = "generada_automaticamente", nullable = false)
    private Boolean generadaAutomaticamente;

    /** Estado actual de la asignación. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsignacion estado = EstadoAsignacion.BORRADOR;

    /**
     * Snapshot del total de cajas disponibles al momento de generar.
     * Permite auditar si el stock cambió entre la generación y la confirmación.
     */
    @Column(name = "total_cajas_disponibles")
    private Integer totalCajasDisponibles;

    /** Snapshot de la suma de cajas solicitadas por todas las iglesias aprobadas. */
    @Column(name = "total_cajas_solicitadas")
    private Integer totalCajasSolicitadas;

    /**
     * Factor de reducción aplicado cuando la demanda supera el stock.
     * {@code 1.0000} cuando hay stock suficiente.
     * {@code < 1.0000} cuando el stock es insuficiente (p. ej. {@code 0.8333} = 83,33% de lo pedido).
     */
    @Column(name = "factor_reduccion", precision = 8, scale = 4)
    private BigDecimal factorReduccion;

    /** Notas del coordinador sobre esta corrida de asignación. */
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Boolean getGeneradaAutomaticamente() { return generadaAutomaticamente; }
    public void setGeneradaAutomaticamente(Boolean generadaAutomaticamente) { this.generadaAutomaticamente = generadaAutomaticamente; }

    public EstadoAsignacion getEstado() { return estado; }
    public void setEstado(EstadoAsignacion estado) { this.estado = estado; }

    public Integer getTotalCajasDisponibles() { return totalCajasDisponibles; }
    public void setTotalCajasDisponibles(Integer totalCajasDisponibles) { this.totalCajasDisponibles = totalCajasDisponibles; }

    public Integer getTotalCajasSolicitadas() { return totalCajasSolicitadas; }
    public void setTotalCajasSolicitadas(Integer totalCajasSolicitadas) { this.totalCajasSolicitadas = totalCajasSolicitadas; }

    public BigDecimal getFactorReduccion() { return factorReduccion; }
    public void setFactorReduccion(BigDecimal factorReduccion) { this.factorReduccion = factorReduccion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
