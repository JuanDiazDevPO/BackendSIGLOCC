package com.siglocc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * Servicio encargado del almacenamiento físico de archivos de soporte (PDF/ZIP)
 * que evidencian los gastos reportados por los equipos.
 *
 * <p><strong>Directorio de almacenamiento:</strong> configurable mediante la propiedad
 * {@code app.storage.path} en {@code application.properties}. Por defecto usa el
 * directorio {@code uploads/} relativo al directorio de trabajo del proceso.</p>
 *
 * <p><strong>Convención de nombres:</strong> Los archivos se almacenan con el formato
 * {@code SOPORTE_EQ{equipoId}_MES{mes}_{anio}{extension}} para garantizar unicidad
 * y facilitar la identificación manual. Si ya existe un archivo con ese nombre
 * (p. ej. por una re-subida), se reemplaza.</p>
 *
 * <p>Ejemplo: un soporte del equipo 7 para marzo 2025 en formato PDF
 * se guardaría como {@code SOPORTE_EQ7_MES3_2025.pdf}.</p>
 */
@Service
public class StorageService {

    /**
     * Directorio raíz donde se almacenan los archivos de soporte.
     * Se crea automáticamente si no existe.
     */
    @Value("${app.storage.path:uploads}")
    private String storagePath;

    /**
     * Almacena el archivo de soporte en disco con la convención de nombre establecida.
     *
     * <p>Pasos internos:</p>
     * <ol>
     *   <li>Extrae la extensión del nombre original del archivo.</li>
     *   <li>Construye el nombre de destino: {@code SOPORTE_EQ{id}_MES{m}_{a}{ext}}.</li>
     *   <li>Crea el directorio de almacenamiento si no existe.</li>
     *   <li>Copia el contenido del archivo al destino, reemplazando si ya existe.</li>
     * </ol>
     *
     * @param archivo   archivo multipart recibido en el request HTTP
     * @param equipoId  ID del equipo dueño del reporte (extraído del JWT)
     * @param mes       mes al que corresponde el soporte (1-12)
     * @param anio      año al que corresponde el soporte
     * @return nombre del archivo almacenado (sin la ruta del directorio)
     * @throws IllegalStateException si ocurre un error de I/O al escribir el archivo
     */
    public String almacenarSoporte(MultipartFile archivo, Integer equipoId, Integer mes, Integer anio) {
        // Extraer y validar extensión del archivo original (solo .pdf o .zip)
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new IllegalArgumentException("El archivo debe tener una extensión válida (.pdf o .zip).");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
        Set<String> extensionesPermitidas = Set.of(".pdf", ".zip");
        if (!extensionesPermitidas.contains(extension)) {
            throw new IllegalArgumentException("Extensión no permitida. Solo se aceptan archivos PDF o ZIP.");
        }

        // Nombre estandarizado según la convención del proyecto
        String nombreArchivo = "SOPORTE_EQ" + equipoId + "_MES" + mes + "_" + anio + extension;

        try {
            Path directorio = Path.of(storagePath);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo almacenar el archivo de soporte: " + e.getMessage());
        }

        return nombreArchivo;
    }

    /**
     * Almacena una foto de evidencia logística con la convención de nombre establecida.
     *
     * <p>La foto se guarda en un subdirectorio según el contexto:</p>
     * <ul>
     *   <li>{@code contenedor/} – fotos de llegada de contenedores.</li>
     *   <li>{@code entrega/}    – fotos de entrega a iglesias.</li>
     *   <li>{@code ninos/}      – fotos de entrega a niños.</li>
     * </ul>
     *
     * <p>Nombre resultante: {@code {contexto}/FOTO_{contexto}_{referenciaId}_{orden}.ext}</p>
     *
     * @param foto        archivo de imagen multipart (JPG, PNG, JPEG)
     * @param contexto    subdirectorio lógico («contenedor», «entrega», «ninos»)
     * @param referenciaId ID del registro al que pertenece la foto
     * @param orden       posición de la foto en el conjunto (1, 2, 3…)
     * @return nombre final del archivo almacenado (incluyendo subdirectorio)
     * @throws IllegalArgumentException si la extensión no es de imagen
     * @throws IllegalStateException    si ocurre un error de I/O
     */
    public String almacenarFotoLogistica(MultipartFile foto, String contexto,
                                         Integer referenciaId, Integer orden) {
        String nombreOriginal = foto.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new IllegalArgumentException("El archivo debe tener una extensión válida.");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
        // El contexto "firma" también acepta PDF (para actas escaneadas)
        Set<String> extensionesPermitidas = "firma".equals(contexto)
                ? Set.of(".jpg", ".jpeg", ".png", ".pdf")
                : Set.of(".jpg", ".jpeg", ".png");
        if (!extensionesPermitidas.contains(extension)) {
            throw new IllegalArgumentException(
                    "Extensión no permitida. Solo se aceptan imágenes JPG o PNG" +
                    ("firma".equals(contexto) ? " o PDF para firmas escaneadas." : "."));
        }

        String nombreArchivo = "FOTO_" + contexto.toUpperCase() + "_"
                + referenciaId + "_" + orden + extension;
        String rutaRelativa  = contexto + "/" + nombreArchivo;

        try {
            Path directorio = Path.of(storagePath, contexto);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreArchivo);
            Files.copy(foto.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo almacenar la foto: " + e.getMessage());
        }

        return rutaRelativa;
    }

    /**
     * Almacena un documento logístico (lista de transportadora o formato ABC).
     *
     * <p>Los documentos se guardan en el subdirectorio {@code docs/} dentro del
     * directorio de almacenamiento configurado.</p>
     *
     * <p>Nombre resultante: {@code docs/DOC_{tipo}_{recepcionId}.ext}</p>
     *
     * @param documento   archivo PDF multipart
     * @param tipo        tipo de documento: «TRANSPORTADORA» o «ABC»
     * @param recepcionId ID de la recepción de contenedor
     * @return nombre final del archivo almacenado (incluyendo subdirectorio)
     * @throws IllegalArgumentException si el archivo no es PDF
     * @throws IllegalStateException    si ocurre un error de I/O
     */
    public String almacenarDocumentoLogistica(MultipartFile documento, String tipo,
                                               Integer recepcionId) {
        String nombreOriginal = documento.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new IllegalArgumentException("El archivo debe tener una extensión válida.");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
        if (!".pdf".equals(extension)) {
            throw new IllegalArgumentException(
                    "Extensión no permitida. Los documentos deben ser PDF.");
        }

        String nombreArchivo = "DOC_" + tipo.toUpperCase() + "_" + recepcionId + extension;
        String rutaRelativa  = "docs/" + nombreArchivo;

        try {
            Path directorio = Path.of(storagePath, "docs");
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreArchivo);
            Files.copy(documento.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo almacenar el documento: " + e.getMessage());
        }

        return rutaRelativa;
    }
}
