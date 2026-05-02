package com.siglocc.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.siglocc.entity.SolicitudAnticipo;
import com.siglocc.entity.TipoCuenta;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Servicio que genera el PDF formal de una solicitud de anticipo.
 *
 * <p>El documento sigue el formato oficial de OCC Colombia:</p>
 * <ul>
 *   <li>Encabezado con nombre de organización y número de solicitud.</li>
 *   <li>Datos del solicitante (ciudad, fecha, nombre, cédula, cargo, equipo).</li>
 *   <li>Datos bancarios del beneficiario.</li>
 *   <li>Monto con valor numérico y en letras.</li>
 *   <li>Destino / descripción del anticipo.</li>
 *   <li>Líneas de firma al pie.</li>
 * </ul>
 */
@Service
public class AnticipoDocumentoService {

    private static final String ORGANIZACION = "Fundación Manitas de Amor y Esperanza";
    private static final String NIT          = "NIT. 900.183.792-4";
    private static final String ONN          = "ONN - COLOMBIA";

    // ── Colores de marca OCC ──────────────────────────────────────────────
    private static final Color VERDE_OCC   = new Color(90, 128, 43);
    private static final Color CARBÓN      = new Color(41, 41, 41);
    private static final Color GRIS_CLARO  = new Color(244, 248, 239);

    // ── Fuentes ───────────────────────────────────────────────────────────
    private static final Font F_TITULO   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, CARBÓN);
    private static final Font F_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, CARBÓN);
    private static final Font F_LABEL    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, CARBÓN);
    private static final Font F_NORMAL   = FontFactory.getFont(FontFactory.HELVETICA, 10, CARBÓN);
    private static final Font F_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CARBÓN);
    private static final Font F_MONTO    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, VERDE_OCC);
    private static final Font F_LETRAS   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CARBÓN);
    private static final Font F_VERDE    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, VERDE_OCC);
    private static final Font F_PEQUEÑA  = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font F_HEADER_BLANCO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
    private static final Font F_HEADER_SUB    = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(200, 220, 180));

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'DE' MMMM yyyy", Locale.forLanguageTag("es"));

    /**
     * Genera los bytes del PDF de la solicitud de anticipo.
     *
     * @param solicitud          entidad persistida (ya tiene ID)
     * @param nombreSolicitante  nombre del usuario que creó la solicitud
     * @param apellidoSolicitante apellido del usuario
     * @param cargo              cargo del solicitante (mapeado desde su rol)
     * @param equipoNombre       nombre del equipo al que pertenece
     * @return arreglo de bytes con el PDF generado
     * @throws IllegalStateException si OpenPDF falla al generar el documento
     */
    public byte[] generarPdf(SolicitudAnticipo solicitud,
                              String nombreSolicitante, String apellidoSolicitante,
                              String cargo, String equipoNombre) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 56, 56, 40, 40);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            agregarEncabezado(doc, solicitud.getId());
            agregarLinea(doc);
            agregarDatosSolicitante(doc, solicitud, nombreSolicitante,
                    apellidoSolicitante, cargo, equipoNombre);
            agregarDatosBancarios(doc, solicitud);
            agregarMonto(doc, solicitud);
            agregarDestino(doc, solicitud);
            agregarFirmas(doc);

            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error al generar el PDF del anticipo: " + e.getMessage());
        }
        return baos.toByteArray();
    }

    // ── Secciones del documento ───────────────────────────────────────────

    private void agregarEncabezado(Document doc, Integer solicitudId) throws DocumentException {
        // Tabla de encabezado: [columna texto izquierda | columna ONN derecha]
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{70, 30});
        header.setSpacingAfter(4);

        // Celda izquierda con fondo verde OCC
        PdfPCell celdaIzq = new PdfPCell();
        celdaIzq.setBackgroundColor(VERDE_OCC);
        celdaIzq.setBorder(Rectangle.NO_BORDER);
        celdaIzq.setPadding(14);
        celdaIzq.addElement(new Paragraph("SIGLOCC", F_HEADER_BLANCO));
        celdaIzq.addElement(new Paragraph("Sistema de Gestión Logística OCC", F_HEADER_SUB));
        header.addCell(celdaIzq);

        // Celda derecha con fondo carbón
        PdfPCell celdaDer = new PdfPCell();
        celdaDer.setBackgroundColor(CARBÓN);
        celdaDer.setBorder(Rectangle.NO_BORDER);
        celdaDer.setPadding(14);
        celdaDer.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaDer.addElement(parrafoAlin(ONN, F_PEQUEÑA, Element.ALIGN_RIGHT));
        header.addCell(celdaDer);

        doc.add(header);

        // Título del documento
        Paragraph titulo = new Paragraph("SOLICITUD DE ANTICIPO #" + solicitudId, F_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(14);
        titulo.setSpacingAfter(2);
        doc.add(titulo);

        Paragraph org = new Paragraph(ORGANIZACION, F_SUBTITULO);
        org.setAlignment(Element.ALIGN_CENTER);
        doc.add(org);

        Paragraph nit = new Paragraph(NIT, F_PEQUEÑA);
        nit.setAlignment(Element.ALIGN_CENTER);
        nit.setSpacingAfter(8);
        doc.add(nit);
    }

    private void agregarLinea(Document doc) throws DocumentException {
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingAfter(10);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(VERDE_OCC);
        cell.setFixedHeight(3f);
        cell.setBorder(Rectangle.NO_BORDER);
        linea.addCell(cell);
        doc.add(linea);
    }

    private void agregarDatosSolicitante(Document doc, SolicitudAnticipo s,
                                          String nombre, String apellido,
                                          String cargo, String equipo) throws DocumentException {
        String fechaStr = s.getFechaSolicitud().format(FORMATO_FECHA).toUpperCase();
        String ciudad   = s.getCiudad() != null ? s.getCiudad().toUpperCase() : "";

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{35, 65});
        tabla.setSpacingAfter(10);

        fila(tabla, "Ciudad y fecha:",     ciudad + ", " + fechaStr);
        fila(tabla, "Nombre solicitante:", (nombre + " " + apellido).toUpperCase());
        fila(tabla, "C.C.:",               formatearCedula(s.getCedula()));
        fila(tabla, "Cargo:",              cargo);
        fila(tabla, "Equipo:",             equipo.toUpperCase());

        doc.add(tabla);
    }

    private void agregarDatosBancarios(Document doc, SolicitudAnticipo s) throws DocumentException {
        String tipoCuentaTexto = tipoCuentaDescripcion(s.getTipoCuenta());

        Paragraph titulo = new Paragraph(
                "Favor consignar a la siguiente cuenta de " + tipoCuentaTexto + ":",
                F_NORMAL_BOLD);
        titulo.setSpacingBefore(4);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        // Tabla con fondo verde claro
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(90);
        tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.setWidths(new float[]{40, 60});
        tabla.setSpacingAfter(12);

        filaBancaria(tabla, "Número de cuenta:", s.getNumeroCuenta());
        filaBancaria(tabla, "Banco:", s.getBanco() != null ? s.getBanco().toUpperCase() : "");
        filaBancaria(tabla, "A nombre de:", s.getNombreTitular() != null
                ? s.getNombreTitular().toUpperCase() : "");
        filaBancaria(tabla, "C.C.#", formatearCedula(s.getCedulaTitular()));

        doc.add(tabla);
    }

    private void agregarMonto(Document doc, SolicitudAnticipo s) throws DocumentException {
        agregarLinea(doc);

        Paragraph monto = new Paragraph(
                "Valor Anticipo:  $" + formatearMonto(s.getMontoSolicitado()) + "=",
                F_MONTO);
        monto.setSpacingBefore(6);
        monto.setSpacingAfter(4);
        doc.add(monto);

        Paragraph letras = new Paragraph(
                numeroALetras(s.getMontoSolicitado()).toUpperCase(),
                F_LETRAS);
        letras.setSpacingAfter(10);
        doc.add(letras);

        agregarLinea(doc);
    }

    private void agregarDestino(Document doc, SolicitudAnticipo s) throws DocumentException {
        Paragraph label = new Paragraph(
                "Destino anticipo #" + s.getId() + ": ", F_NORMAL_BOLD);
        label.setSpacingBefore(8);

        Chunk destino = new Chunk(
                s.getDescripcion() != null ? s.getDescripcion().toUpperCase() : "",
                F_NORMAL);
        label.add(destino);
        label.setSpacingAfter(30);
        doc.add(label);
    }

    private void agregarFirmas(Document doc) throws DocumentException {
        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(90);
        firmas.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell f1 = celda(firmas);
        f1.addElement(parrafo("________________________________", F_NORMAL));
        f1.addElement(parrafo("V.B. Coordinador Nacional", F_VERDE));
        f1.addElement(parrafo("de Liderazgo", F_VERDE));
        firmas.addCell(f1);

        PdfPCell f2 = celda(firmas);
        f2.addElement(parrafo("________________________________", F_NORMAL));
        f2.addElement(parrafo("Autoriza Coordinación Nacional", F_VERDE));
        f2.addElement(parrafo("de Finanzas", F_VERDE));
        firmas.addCell(f2);

        doc.add(firmas);

        Paragraph pie = new Paragraph("Documento generado por SIGLOCC · ONN Colombia", F_PEQUEÑA);
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(20);
        doc.add(pie);
    }

    // ── Helpers de construcción de tabla ─────────────────────────────────

    private void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell cEtiqueta = new PdfPCell(new Phrase(etiqueta, F_LABEL));
        cEtiqueta.setBorder(Rectangle.NO_BORDER);
        cEtiqueta.setPaddingBottom(5);
        tabla.addCell(cEtiqueta);

        PdfPCell cValor = new PdfPCell(new Phrase(valor != null ? valor : "", F_NORMAL));
        cValor.setBorder(Rectangle.NO_BORDER);
        cValor.setPaddingBottom(5);
        tabla.addCell(cValor);
    }

    private void filaBancaria(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell cEtiqueta = new PdfPCell(new Phrase(etiqueta, F_LABEL));
        cEtiqueta.setBackgroundColor(GRIS_CLARO);
        cEtiqueta.setBorderColor(new Color(220, 225, 210));
        cEtiqueta.setPadding(6);
        tabla.addCell(cEtiqueta);

        PdfPCell cValor = new PdfPCell(new Phrase(valor != null ? valor : "", F_NORMAL));
        cValor.setBackgroundColor(Color.WHITE);
        cValor.setBorderColor(new Color(220, 225, 210));
        cValor.setPadding(6);
        tabla.addCell(cValor);
    }

    private PdfPCell celda(PdfPTable tabla) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(10);
        return cell;
    }

    private Paragraph parrafo(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setSpacingAfter(2);
        return p;
    }

    private Paragraph parrafoAlin(String texto, Font font, int alineacion) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(alineacion);
        return p;
    }

    // ── Helpers de formato ────────────────────────────────────────────────

    private String tipoCuentaDescripcion(TipoCuenta tipo) {
        if (tipo == null) return "ahorros";
        return switch (tipo) {
            case AHORROS   -> "ahorros";
            case CORRIENTE -> "corriente";
            case NEQUI     -> "NEQUI";
            case DAVIPLATA -> "Daviplata";
        };
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) return "0";
        return String.format(Locale.forLanguageTag("es"), "%,.0f", monto);
    }

    private String formatearCedula(String cedula) {
        return cedula != null ? cedula : "";
    }

    // ── Conversión numérica a letras (español) ────────────────────────────

    private String numeroALetras(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) == 0) {
            return "CERO PESOS M/C";
        }
        long entero = monto.longValue();
        return convertirEntero(entero) + " PESOS M/C";
    }

    private String convertirEntero(long n) {
        if (n >= 1_000_000_000L) {
            long miles = n / 1_000_000_000L;
            long resto = n % 1_000_000_000L;
            String parte = miles == 1
                    ? "MIL MILLONES"
                    : convertirMiles(miles) + " MIL MILLONES";
            return resto == 0 ? parte : parte + " " + convertirMiles(resto);
        }
        if (n >= 1_000_000L) {
            long millones = n / 1_000_000L;
            long resto    = n % 1_000_000L;
            String parte  = millones == 1
                    ? "UN MILLÓN"
                    : convertirCientos(millones) + " MILLONES";
            return resto == 0 ? parte : parte + " " + convertirMiles(resto);
        }
        return convertirMiles(n);
    }

    private String convertirMiles(long n) {
        if (n >= 1000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String parte = miles == 1 ? "MIL" : convertirCientos(miles) + " MIL";
            return resto == 0 ? parte : parte + " " + convertirCientos(resto);
        }
        return convertirCientos(n);
    }

    private String convertirCientos(long n) {
        if (n == 100) return "CIEN";
        if (n >= 100) {
            long cientos = n / 100;
            long resto   = n % 100;
            String parte = CIENTOS[(int) cientos];
            return resto == 0 ? parte : parte + " " + convertirDecenas(resto);
        }
        return convertirDecenas(n);
    }

    private String convertirDecenas(long n) {
        if (n <= 20) return UNIDADES[(int) n];
        long decena = n / 10;
        long unidad = n % 10;
        if (unidad == 0) return DECENAS[(int) decena];
        if (decena == 2) return "VEINTI" + UNIDADES[(int) unidad].toLowerCase();
        return DECENAS[(int) decena] + " Y " + UNIDADES[(int) unidad];
    }

    private static final String[] UNIDADES = {
        "CERO", "UNO", "DOS", "TRES", "CUATRO",
        "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE",
        "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };

    private static final String[] DECENAS = {
        "", "", "VEINTE", "TREINTA", "CUARENTA",
        "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CIENTOS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
        "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };
}
