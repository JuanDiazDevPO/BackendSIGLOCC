package com.siglocc.entity;

import jakarta.persistence.*;

/**
 * Catálogo de ítems logísticos que se distribuyen durante la operación OCC.
 *
 * <p>Mapea la tabla {@code tipos_item}. Cada fila describe un tipo de material:</p>
 * <ul>
 *   <li><strong>OE</strong> – Caja Operation Christmas (para niños).</li>
 *   <li><strong>FOLLETO</strong> – Folleto informativo para pastores (Momento 1).</li>
 *   <li><strong>GM</strong> – Guía Ministerial para maestros (Momento 2).</li>
 *   <li><strong>MPG</strong> – Libro presentación del evangelio para maestros (Momento 2).</li>
 *   <li><strong>EMR</strong> – Literatura para niños (Momento 3).</li>
 *   <li><strong>LGA</strong> – Literatura para niños (Momento 3).</li>
 *   <li><strong>NT</strong> – Nuevo Testamento para niños (Momento 3).</li>
 * </ul>
 *
 * <p>Los ítems con {@code aplica_ninos = true} se incluyen en el detalle de entrega
 * a iglesias (Momento 3). Los demás solo se registran en los momentos correspondientes.</p>
 */
@Entity
@Table(name = "tipos_item")
public class TipoItem {

    /** Identificador único autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código abreviado del ítem (OE, FOLLETO, GM, MPG, EMR, LGA, NT).
     * Único en la tabla; se usa como referencia en los detalles de entrega.
     */
    @Column(unique = true, nullable = false, length = 10)
    private String codigo;

    /** Nombre completo descriptivo del ítem. */
    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    /**
     * Indica si el ítem se entrega a los niños a través de las iglesias.
     * {@code true} para OE, EMR, LGA, NT. {@code false} para FOLLETO, GM, MPG.
     */
    @Column(name = "aplica_ninos", nullable = false)
    private Boolean aplicaNinos;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public Boolean getAplicaNinos() { return aplicaNinos; }
    public void setAplicaNinos(Boolean aplicaNinos) { this.aplicaNinos = aplicaNinos; }
}
