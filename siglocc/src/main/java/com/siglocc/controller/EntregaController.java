package com.siglocc.controller;

import com.siglocc.dto.EntregaRequest;
import com.siglocc.dto.EntregaResponse;
import com.siglocc.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del Momento 3 – La Entrega, y los Momentos B y C de fotos.
 *
 * <p>Expone siete endpoints:</p>
 * <ol>
 *   <li>{@code POST  /api/v1/logistica/entregas} – crear acta de entrega.</li>
 *   <li>{@code PATCH /api/v1/logistica/entregas/{id}/completar} – marcar COMPLETADA o PARCIAL.</li>
 *   <li>{@code POST  /api/v1/logistica/entregas/{id}/firma} – adjuntar firma.</li>
 *   <li>{@code POST  /api/v1/logistica/entregas/{id}/fotos} – fotos Momento B (entrega a iglesia).</li>
 *   <li>{@code POST  /api/v1/logistica/entregas/fotos-ninos} – fotos Momento C (entrega a niños).</li>
 *   <li>{@code GET   /api/v1/logistica/entregas?temporadaId=} – listar actas.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/logistica/entregas")
@Tag(name = "Logística – Momento 3 Entrega", description = "Actas de entrega y fotos de evidencia")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    /**
     * Crea el acta de entrega de cajas y literatura para una iglesia aprobada.
     * La iglesia debe estar en estado APROBADA y no tener un acta previa en esa temporada.
     */
    @Operation(summary = "Crear acta de entrega",
               description = "Solo iglesias APROBADAS. Una acta por iglesia por temporada. " +
                             "El equipoId se extrae del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<EntregaResponse> crearEntrega(@RequestBody EntregaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregaService.crearEntrega(request));
    }

    /**
     * Marca el acta como COMPLETADA o PARCIAL y confirma la recepción.
     *
     * @param parcial si es {@code true}, el estado pasa a PARCIAL; si es {@code false}, a COMPLETADA
     */
    @Operation(summary = "Completar acta de entrega",
               description = "parcial=true → PARCIAL; parcial=false (default) → COMPLETADA.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/completar")
    public ResponseEntity<EntregaResponse> completar(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean parcial) {
        return ResponseEntity.ok(entregaService.completarEntrega(id, parcial));
    }

    /**
     * Adjunta la firma del receptor al acta de entrega (JPG/PNG).
     */
    @Operation(summary = "Subir firma del acta",
               description = "Adjunta la firma digital o escaneada al acta de entrega.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/{id}/firma", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntregaResponse> subirFirma(
            @PathVariable Integer id,
            @RequestParam MultipartFile firma) {
        return ResponseEntity.ok(entregaService.subirFirma(id, firma));
    }

    /**
     * Sube una foto de evidencia del momento de entrega en el punto de distribución (Momento B).
     */
    @Operation(summary = "Subir foto de entrega a iglesia (Momento B)",
               description = "Cualquier equipo puede cargar fotos. Orden automático.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFotoEntrega(
            @PathVariable Integer id,
            @RequestParam MultipartFile foto) {
        String rutaArchivo = entregaService.subirFotoEntrega(id, foto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", rutaArchivo));
    }

    /**
     * Sube una foto de evidencia de la entrega a los niños en la iglesia (Momento C).
     */
    @Operation(summary = "Subir foto de entrega a niños (Momento C)",
               description = "Fotos del momento en que los niños reciben las cajas. " +
                             "Cualquier equipo puede cargarlas.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/fotos-ninos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFotoNinos(
            @RequestParam Integer iglesiaId,
            @RequestParam Integer temporadaId,
            @RequestParam MultipartFile foto) {
        String rutaArchivo = entregaService.subirFotoNinos(iglesiaId, temporadaId, foto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", rutaArchivo));
    }

    /**
     * Lista las actas de entrega visibles para el usuario autenticado en una temporada.
     */
    @Operation(summary = "Listar actas de entrega",
               description = "ENL ve todas; ERLE ve su clúster; ERL ve las suyas.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<EntregaResponse>> listar(@RequestParam Integer temporadaId) {
        return ResponseEntity.ok(entregaService.listar(temporadaId));
    }

}
