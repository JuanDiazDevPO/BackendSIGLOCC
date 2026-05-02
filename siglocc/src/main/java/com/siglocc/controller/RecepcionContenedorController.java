package com.siglocc.controller;

import com.siglocc.dto.RecepcionContenedorRequest;
import com.siglocc.dto.RecepcionContenedorResponse;
import com.siglocc.service.RecepcionContenedorService;
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
 * Controlador REST para el registro de llegada de contenedores (Momento A de fotos).
 *
 * <p>Expone cinco endpoints:</p>
 * <ol>
 *   <li>{@code POST  /api/v1/logistica/recepciones} – registrar llegada de contenedor.</li>
 *   <li>{@code POST  /api/v1/logistica/recepciones/{id}/documentos} – adjuntar documento.</li>
 *   <li>{@code POST  /api/v1/logistica/recepciones/{id}/fotos} – subir foto (máx. 4).</li>
 *   <li>{@code GET   /api/v1/logistica/recepciones?temporadaId=} – listar.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/logistica/recepciones")
@Tag(name = "Logística – Recepción Contenedores", description = "Registro de llegada de contenedores y fotos")
public class RecepcionContenedorController {

    private final RecepcionContenedorService recepcionService;

    public RecepcionContenedorController(RecepcionContenedorService recepcionService) {
        this.recepcionService = recepcionService;
    }

    /**
     * Registra la llegada de un contenedor a un punto de entrega.
     */
    @Operation(summary = "Registrar llegada de contenedor",
               description = "El equipoId se extrae del JWT. Los documentos y fotos " +
                             "se adjuntan con endpoints separados.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<RecepcionContenedorResponse> registrar(
            @RequestBody RecepcionContenedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recepcionService.registrar(request));
    }

    /**
     * Adjunta un documento a la recepción (lista transportadora o formato ABC).
     *
     * @param tipo «TRANSPORTADORA» o «ABC»
     */
    @Operation(summary = "Subir documento de recepción",
               description = "tipo=TRANSPORTADORA para la lista de entrega; tipo=ABC para el formato interno.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecepcionContenedorResponse> subirDocumento(
            @PathVariable Integer id,
            @RequestParam String tipo,
            @RequestParam MultipartFile documento) {
        return ResponseEntity.ok(recepcionService.subirDocumento(id, tipo, documento));
    }

    /**
     * Sube una foto de evidencia de la llegada del contenedor (máximo 4 fotos).
     * La primera foto debe mostrar el número del contenedor en la puerta.
     */
    @Operation(summary = "Subir foto de contenedor",
               description = "Máximo 4 fotos por recepción. Orden automático. " +
                             "La foto 1 debe mostrar el número en la puerta.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFoto(
            @PathVariable Integer id,
            @RequestParam MultipartFile foto,
            @RequestParam(required = false) String descripcion) {
        String rutaArchivo = recepcionService.subirFoto(id, foto, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", rutaArchivo));
    }

    /**
     * Lista las recepciones visibles para el usuario autenticado en una temporada.
     */
    @Operation(summary = "Listar recepciones de contenedores",
               description = "Filtrado automático por jerarquía del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<RecepcionContenedorResponse>> listar(
            @RequestParam Integer temporadaId) {
        return ResponseEntity.ok(recepcionService.listar(temporadaId));
    }

}
