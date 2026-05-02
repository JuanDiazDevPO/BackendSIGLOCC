package com.siglocc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Punto de entrega donde llegan los contenedores y desde donde se distribuyen
 * las cajas a las iglesias.
 *
 * <p>Mapea la tabla {@code puntos_entrega}. Solo los equipos ERLE pueden registrar
 * puntos de entrega. Cada punto incluye:</p>
 * <ul>
 *   <li>Información de ubicación (departamento, ciudad, dirección, coordenadas GPS).</li>
 *   <li>Lista de verificación de 5 condiciones logísticas del lugar.</li>
 * </ul>
 *
 * <p>Las coordenadas GPS ({@code coordenadasLat} y {@code coordenadasLng}) permiten
 * al frontend generar automáticamente un enlace de Google Maps con el formato
 * {@code https://maps.google.com/?q={lat},{lng}}.</p>
 */
@Entity
@Table(name = "puntos_entrega")
public class PuntoEntrega {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre descriptivo del lugar (ej: «Bodega norte Medellín»). */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Departamento donde se ubica el punto. */
    @Column(nullable = false, length = 50)
    private String departamento;

    /** Ciudad donde se ubica el punto. */
    @Column(nullable = false, length = 50)
    private String ciudad;

    /** Dirección detallada del punto de entrega. */
    @Column(columnDefinition = "TEXT")
    private String direccion;

    /** Latitud GPS del punto. Precisión de 6 decimales (~11 cm de precisión). */
    @Column(name = "coordenadas_lat", precision = 9, scale = 6)
    private BigDecimal coordenadasLat;

    /** Longitud GPS del punto. Precisión de 6 decimales. */
    @Column(name = "coordenadas_lng", precision = 9, scale = 6)
    private BigDecimal coordenadasLng;

    // ── Lista de verificación de condiciones logísticas ──────────────────────

    /** ¿El lugar tiene restricciones de movilidad para camiones? */
    @Column(name = "restriccion_movilidad", nullable = false)
    private Boolean restriccionMovilidad = false;

    /** ¿El lugar tiene altura adecuada para las cuerdas de descargue? */
    @Column(name = "altura_cuerdas", nullable = false)
    private Boolean alturaCuerdas = false;

    /** ¿El techo no tiene tejas rotas ni riesgo de filtración? */
    @Column(name = "no_tejas_rotas", nullable = false)
    private Boolean noTejasRotas = false;

    /** ¿El lugar es seguro para almacenar material durante la operación? */
    @Column(name = "lugar_seguro", nullable = false)
    private Boolean lugarSeguro = false;

    /** ¿El lugar tiene fácil acceso para los equipos de distribución? */
    @Column(name = "facil_acceso", nullable = false)
    private Boolean facilAcceso = false;

    // ── Contexto del registro ────────────────────────────────────────────────

    /**
     * ID del equipo ERLE que registra el punto.
     * Se extrae del JWT al momento de la creación.
     */
    @Column(name = "equipo_id", nullable = false)
    private Integer equipoId;

    /** ID de la temporada a la que pertenece este punto de entrega. */
    @Column(name = "temporada_id", nullable = false)
    private Integer temporadaId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public BigDecimal getCoordenadasLat() { return coordenadasLat; }
    public void setCoordenadasLat(BigDecimal coordenadasLat) { this.coordenadasLat = coordenadasLat; }

    public BigDecimal getCoordenadasLng() { return coordenadasLng; }
    public void setCoordenadasLng(BigDecimal coordenadasLng) { this.coordenadasLng = coordenadasLng; }

    public Boolean getRestriccionMovilidad() { return restriccionMovilidad; }
    public void setRestriccionMovilidad(Boolean restriccionMovilidad) { this.restriccionMovilidad = restriccionMovilidad; }

    public Boolean getAlturaCuerdas() { return alturaCuerdas; }
    public void setAlturaCuerdas(Boolean alturaCuerdas) { this.alturaCuerdas = alturaCuerdas; }

    public Boolean getNoTejasRotas() { return noTejasRotas; }
    public void setNoTejasRotas(Boolean noTejasRotas) { this.noTejasRotas = noTejasRotas; }

    public Boolean getLugarSeguro() { return lugarSeguro; }
    public void setLugarSeguro(Boolean lugarSeguro) { this.lugarSeguro = lugarSeguro; }

    public Boolean getFacilAcceso() { return facilAcceso; }
    public void setFacilAcceso(Boolean facilAcceso) { this.facilAcceso = facilAcceso; }

    public Integer getEquipoId() { return equipoId; }
    public void setEquipoId(Integer equipoId) { this.equipoId = equipoId; }

    public Integer getTemporadaId() { return temporadaId; }
    public void setTemporadaId(Integer temporadaId) { this.temporadaId = temporadaId; }
}
