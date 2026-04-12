package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad de detalle que representa un rubro específico dentro de un reporte mensual.
 *
 * <p>Mapea la tabla {@code reporte_detalles}. Cada fila asocia una categoría de gasto
 * a un reporte cabezote con el monto real ejecutado en ese rubro.</p>
 *
 * <p>La suma de todos los {@code monto_gastado} de un reporte cuyos detalles pertenezcan
 * a familia {@code E} o {@code M} alimenta el cálculo del ejecutado en las vistas
 * financieras (una vez que el reporte alcanza el estado {@link EstadoReporte#APROBADO}).</p>
 *
 * <p><strong>Transaccionalidad:</strong> Los detalles se guardan dentro de la misma
 * transacción del método {@code crearReporte}. Si falla algún detalle (p. ej. monto
 * negativo o categoría inexistente), toda la operación se revierte y el cabezote
 * tampoco se persiste.</p>
 */
@Entity
@Table(name = "reporte_detalles")
public class ReporteDetalle {

    /** Identificador único autoincremental del detalle. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID del reporte cabezote al que pertenece este detalle.
     * Corresponde a {@link ReporteMensual#getId()}.
     */
    @Column(name = "reporte_id", nullable = false)
    private Integer reporteId;

    /**
     * Código de la categoría de gasto reportada (p. ej. {@code E-1}, {@code M-2}).
     * Referencia la clave primaria de {@link ReporteCategoria}.
     */
    @Column(name = "categoria_codigo", nullable = false, length = 10)
    private String categoriaCodigo;

    /**
     * Monto real gastado en este rubro, en COP.
     * Debe ser mayor o igual a cero.
     */
    @Column(name = "monto_gastado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoGastado;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getReporteId() { return reporteId; }
    public void setReporteId(Integer reporteId) { this.reporteId = reporteId; }

    public String getCategoriaCodigo() { return categoriaCodigo; }
    public void setCategoriaCodigo(String categoriaCodigo) { this.categoriaCodigo = categoriaCodigo; }

    public BigDecimal getMontoGastado() { return montoGastado; }
    public void setMontoGastado(BigDecimal montoGastado) { this.montoGastado = montoGastado; }
}
